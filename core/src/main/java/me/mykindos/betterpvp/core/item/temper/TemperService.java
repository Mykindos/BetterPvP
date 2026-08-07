package me.mykindos.betterpvp.core.item.temper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.temper.TemperComponent;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.component.PermanentComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the live temper of the item each player is holding: spends it while an ability runs, brings
 * it back when they stop, draws the bar on their action bar, and decides when it is safe to write
 * the value back onto the stack.
 * <p>
 * <b>On write timing.</b> Temper is deliberately not persisted on every drain, nor on a timer.
 * Rewriting a held stack's persistent data mid-channel makes the client treat it as a new item and
 * abandon the use animation, which would cut the very ability that is draining it. So the value
 * lives in memory for as long as the player holds the item, and is written to the stack only at the
 * moments where something other than this service could observe or move it: switching hotbar slots,
 * opening or closing an inventory, dropping the item, dying, or disconnecting.
 * <p>
 * The cost of that is a server crash losing whatever was spent since the last of those moments,
 * which returns the item closer to full rather than emptier.
 */
@Singleton
@BPvPListener
public class TemperService implements Listener {

    /**
     * Drain is charged by elapsed time rather than per tick, so it stays rate-correct whatever
     * drives the hold. This caps the very first step after a pause, so a channel that resumes after
     * a long idle spends one step rather than the whole gap.
     */
    private static final double MAX_DRAIN_STEP_SECONDS = 0.25;
    /**
     * Length of the action bar readout. Twice the tooltip's, since the action bar is the one being
     * watched during a fight and has the room for it.
     */
    private static final int BAR_LENGTH = 30;
    /** Below the cooldown readout, which is the more urgent thing to say. */
    private static final int ACTION_BAR_PRIORITY = 900;

    private final ItemFactory itemFactory;
    private final ClientManager clientManager;

    /** The tempered item each player currently holds, keyed by player. */
    private final Map<UUID, Wielded> wielded = new ConcurrentHashMap<>();

    /**
     * Item keys already known to carry no temper. Temper is declared by the base item, so one look
     * settles it for that item forever and the tick loop can skip rebuilding an instance for every
     * ordinary sword every player is holding.
     */
    private final Set<String> untempered = ConcurrentHashMap.newKeySet();

    /** Players whose action bar already carries the temper bar. */
    private final Set<UUID> displaying = ConcurrentHashMap.newKeySet();

    private final PermanentComponent actionBar = new PermanentComponent(this::renderActionBar);

    @Inject
    private TemperService(ItemFactory itemFactory, ClientManager clientManager) {
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
    }

    /**
     * The temper the given item holds right now, from 0 to 1. Reads through the live value when the
     * item is the one its wielder is holding, so a bar being spent right now reads as such.
     *
     * @param wielder the player holding it, or null if it is not in anyone's hand
     */
    public double getTemper(@Nullable Player wielder, @NotNull ItemInstance instance) {
        final long now = System.currentTimeMillis();
        return resolveComponent(wielder, instance).map(component -> component.temperAt(now)).orElse(1d);
    }

    /** Whether the item has nothing left to spend. */
    public boolean isSpent(@Nullable Player wielder, @NotNull ItemInstance instance) {
        final long now = System.currentTimeMillis();
        return resolveComponent(wielder, instance).map(component -> component.isSpentAt(now)).orElse(false);
    }

    /**
     * Whether the item has enough temper left for its ability to be started. Mid-ability draining is
     * gated by {@link #drain} instead, so a channel is not cut the instant it dips below the
     * activation minimum.
     */
    public boolean canActivate(@Nullable Player wielder, @NotNull ItemInstance instance) {
        final long now = System.currentTimeMillis();
        return resolveComponent(wielder, instance)
                .map(component -> {
                    final double temper = component.temperAt(now);
                    return temper > 0 && temper >= component.getProfile().getMinimumToUse();
                })
                .orElse(true);
    }

    /**
     * Spends temper for one step of a continuous ability, charged by the time elapsed since the last
     * drain.
     *
     * @param multiplier scales the profile's drain rate for this step, for abilities that cost more
     *                   in some states than others
     * @return false if the item had nothing left to spend, in which case the ability should stop
     */
    public boolean drain(@NotNull Player player, @NotNull ItemInstance instance, double multiplier) {
        final Wielded state = track(player, instance);
        if (state == null) {
            return true; // Not a tempered item; nothing gates it
        }

        final TemperComponent component = state.component;
        final long now = System.currentTimeMillis();
        if (component.isSpentAt(now)) {
            return false;
        }

        final double seconds = Math.min(MAX_DRAIN_STEP_SECONDS, Math.max(0, now - component.getLastDrainTime()) / 1000d);
        component.drain(component.getProfile().drainPerSecond() * seconds * multiplier, now);
        state.dirty = true;
        return true;
    }

    /**
     * Spends a fixed fraction of the bar in one go, for abilities that cost something on a discrete
     * event rather than continuously.
     *
     * @return false if the item had nothing left to spend
     */
    public boolean spend(@NotNull Player player, @NotNull ItemInstance instance, double fraction) {
        final Wielded state = track(player, instance);
        if (state == null) {
            return true;
        }

        final TemperComponent component = state.component;
        final long now = System.currentTimeMillis();
        if (component.isSpentAt(now)) {
            return false;
        }

        component.drain(fraction, now);
        state.dirty = true;
        return true;
    }

    /**
     * Refills the item completely, for effects that restore an item rather than wait out its
     * recovery.
     */
    public void restore(@NotNull Player player, @NotNull ItemInstance instance) {
        final Wielded state = track(player, instance);
        if (state == null) {
            return;
        }

        state.component.recover(1, System.currentTimeMillis());
        state.dirty = true;
    }

    /**
     * Starts tracking the item a player is holding, or returns the state already being tracked for
     * it. Returns null if the item carries no temper.
     */
    private @Nullable Wielded track(@NotNull Player player, @NotNull ItemInstance instance) {
        final Optional<TemperComponent> component = instance.getComponent(TemperComponent.class);
        if (component.isEmpty()) {
            return null;
        }

        final UUID playerId = player.getUniqueId();
        final Wielded existing = wielded.get(playerId);
        if (existing != null && existing.matches(instance)) {
            return existing;
        }

        if (existing != null) {
            flush(existing);
        }

        final Wielded state = new Wielded(instance, component.get(), itemKeyOf(instance.getItemStack()));
        wielded.put(playerId, state);
        return state;
    }

    /**
     * The live component for an item if its wielder is holding it, otherwise the item's own — which
     * for a stowed item is exactly right, since a stowed item is not being spent by anyone.
     */
    private Optional<TemperComponent> resolveComponent(@Nullable Player wielder, @NotNull ItemInstance instance) {
        final Wielded state = wielder == null ? null : wielded.get(wielder.getUniqueId());
        if (state != null && state.matches(instance)) {
            return Optional.of(state.component);
        }
        return instance.getComponent(TemperComponent.class);
    }

    /**
     * Keeps every held bar current: recovers conditional profiles that only advance while their
     * wielder satisfies them, and flushes settled values back onto their stacks.
     */
    @UpdateEvent(delay = 100)
    public void tick() {
        final long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            showBar(player);
            tickPlayer(player, now);
        }
    }

    /**
     * Puts the bar on a player's action bar once. Done from the tick rather than on join so that
     * players who were already online when this was loaded get one too.
     */
    private void showBar(@NotNull Player player) {
        if (!displaying.add(player.getUniqueId())) {
            return;
        }
        clientManager.search().online(player).getGamer().getActionBar().add(ACTION_BAR_PRIORITY, actionBar);
    }

    private void tickPlayer(@NotNull Player player, long now) {
        final ItemStack held = player.getInventory().getItemInMainHand();
        final String heldKey = itemKeyOf(held);
        Wielded state = wielded.get(player.getUniqueId());

        if (state != null && !state.itemKey.equals(heldKey)) {
            // They swapped to something else; settle what they were holding and pick up whatever
            // is in their hand now, in this same tick so the bar follows the swap immediately.
            release(player.getUniqueId());
            state = null;
        }

        if (state == null) {
            if (heldKey != null && !untempered.contains(heldKey)) {
                // Building the instance is the expensive part, so it only happens for an item we
                // have not already ruled out.
                itemFactory.fromItemStack(held).ifPresent(instance -> {
                    if (track(player, instance) == null) {
                        untempered.add(heldKey);
                    }
                });
            }
            return;
        }

        recover(player, state, now);
    }

    /**
     * Advances a conditional profile by the time since the last tick. Unconditional profiles are
     * left alone — {@link TemperComponent#temperAt(long)} resolves those from the clock, and
     * advancing them here as well would count the same seconds twice.
     */
    private void recover(@NotNull Player player, @NotNull Wielded state, long now) {
        final TemperComponent component = state.component;
        final TemperProfile profile = component.getProfile();
        final long lastTick = state.lastTick;
        state.lastTick = now;

        if (profile.recoversWhileStowed() || component.getTemper() >= 1 || lastTick == 0) {
            return;
        }

        if (now - component.getLastDrainTime() < profile.getRecoveryDelay() * 1000 || !profile.canRecover(player)) {
            return;
        }

        component.recover((now - lastTick) / 1000d * profile.recoveryPerSecond(), now);
        state.dirty = true;
    }

    /** Writes the live value back onto the stack. */
    private void flush(@NotNull Wielded state) {
        state.dirty = false;
        state.instance.serializeAllComponentsToItemStack();
    }

    private void release(@NotNull UUID playerId) {
        final Wielded state = wielded.remove(playerId);
        if (state != null && state.dirty) {
            flush(state);
        }
    }

    /** The custom item key on a stack, or null if it is not one of ours. */
    private @Nullable String itemKeyOf(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
    }

    private @Nullable Component renderActionBar(@NotNull Gamer gamer) {
        final Player player = gamer.getPlayer();
        if (player == null) {
            return null;
        }

        final Wielded state = wielded.get(player.getUniqueId());
        if (state == null) {
            return null;
        }

        final double temper = state.component.temperAt(System.currentTimeMillis());
        return ProgressBar.withLength((float) temper, BAR_LENGTH).build();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        release(event.getPlayer().getUniqueId());
        displaying.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        release(event.getPlayer().getUniqueId());
    }

    /**
     * Settled before the player can move the stack, so whatever they pick up carries the value it
     * had in their hand rather than the one it was last written with.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        release(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        release(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        release(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        release(event.getEntity().getUniqueId());
    }

    /**
     * The tempered item a player is holding. The component is the live one — the interaction, the
     * recovery tick and the action bar all read and write this single object, and the stack only
     * learns about it when the value is flushed.
     */
    private static final class Wielded {

        private final ItemInstance instance;
        private final TemperComponent component;
        private final String itemKey;
        private long lastTick;
        private boolean dirty;

        private Wielded(ItemInstance instance, TemperComponent component, String itemKey) {
            this.instance = instance;
            this.component = component;
            this.itemKey = itemKey == null ? "" : itemKey;
        }

        /**
         * Identity is the item's own unique id rather than its stack contents, because a stack's
         * data legitimately changes mid-channel — Voltic Bash damages its own item every second
         * while it runs — and comparing contents would read that as a different sword.
         */
        private boolean matches(ItemInstance other) {
            if (instance == other) {
                return true;
            }

            final Optional<UUIDProperty> mine = instance.getComponent(UUIDProperty.class);
            final Optional<UUIDProperty> theirs = other.getComponent(UUIDProperty.class);
            if (mine.isPresent() && theirs.isPresent()) {
                return mine.get().getUniqueId().equals(theirs.get().getUniqueId());
            }
            return instance.getBaseItem() == other.getBaseItem();
        }
    }
}
