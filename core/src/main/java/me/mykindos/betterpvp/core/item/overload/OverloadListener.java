package me.mykindos.betterpvp.core.item.overload;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.types.negative.OverloadedEffect;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.component.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Penalises players who keep more high-rarity weapons drawn than the configured cap allows.
 * <p>
 * The applied level is driven by a per-player meter that ramps toward the number of weapons the player is
 * over the cap by. The ramp is the grace period - loot picked up and stowed within a few seconds never
 * produces an effect at all - and it is why this reconciles on a timer rather than purely on events. Joining
 * and respawning snap the meter straight to its target instead of ramping, so relogging cannot buy a fresh
 * grace window.
 */
@BPvPListener
@Singleton
public class OverloadListener implements Listener {

    private static final String EFFECT_NAME = "Overload";
    private static final int ACTION_BAR_PRIORITY = 150;
    private static final long TICK_MILLIS = 100L;
    private static final int STORAGE_SLOTS = 36;
    private static final int HOTBAR_SLOTS = 9;

    private final Core core;
    private final ItemFactory itemFactory;
    private final EffectManager effectManager;
    private final ClientManager clientManager;
    private final Map<UUID, OverloadData> data = new HashMap<>();

    @Inject
    @Config(path = "core.overload.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    @Config(path = "core.overload.cap", defaultValue = "3")
    private int cap;

    @Inject
    @Config(path = "core.overload.speed-reduction-per-level", defaultValue = "0.10")
    private double speedReductionPerLevel;

    @Inject
    @Config(path = "core.overload.max-level", defaultValue = "6")
    private int maxLevel;

    @Inject
    @Config(path = "core.overload.rise-seconds-per-level", defaultValue = "1.0")
    private double riseSecondsPerLevel;

    @Inject
    @Config(path = "core.overload.fall-seconds-per-level", defaultValue = "0.0")
    private double fallSecondsPerLevel;

    @Inject
    @Config(path = "core.overload.rarity-threshold", defaultValue = "LEGENDARY")
    private String rarityThreshold;

    @Inject
    @Config(path = "core.overload.count-armor", defaultValue = "false")
    private boolean countArmor;

    @Inject
    @Config(path = "core.overload.count-inventory", defaultValue = "false")
    private boolean countInventory;

    @Inject
    @Config(path = "core.overload.warn-message", defaultValue = "true")
    private boolean warnMessage;

    @Inject
    @Config(path = "core.overload.actionbar", defaultValue = "true")
    private boolean actionBar;

    @Inject
    private OverloadListener(Core core, ItemFactory itemFactory, EffectManager effectManager, ClientManager clientManager) {
        this.core = core;
        this.itemFactory = itemFactory;
        this.effectManager = effectManager;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        snapNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // Respawning rebuilds the player entity, taking the transient movement modifier with it
        snapNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final OverloadData playerData = data.remove(player.getUniqueId());
        if (playerData != null && playerData.actionBarComponent != null) {
            final Client client = clientManager.search().online(player);
            if (client != null) {
                client.getGamer().getActionBar().remove(playerData.actionBarComponent);
            }
        }

        effectManager.removeEffect(player, EffectTypes.OVERLOADED, EFFECT_NAME, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        markDirtyNextTick(event.getPlayer());
    }

    /**
     * Keeps automatic pickups off the hotbar once a player is at the cap.
     * <p>
     * Vanilla fills slots 0-8 before the backpack, so looting a corpse would otherwise apply the penalty for
     * an act the player never chose - and would let a dying opponent slow their killer by dropping weapons at
     * their feet. Deliberately dragging a weapon onto the hotbar is left alone; the penalty should only ever
     * be self-inflicted.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!enabled || countInventory) {
            return; // With the whole inventory counted there is nowhere to reroute to
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        final ItemStack stack = event.getItem().getItemStack();
        if (!counts(stack) || countWeapons(player) < cap) {
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        for (int slot = HOTBAR_SLOTS; slot < STORAGE_SLOTS; slot++) {
            final ItemStack existing = inventory.getItem(slot);
            if (existing != null && !existing.getType().isAir()) {
                continue;
            }

            event.setCancelled(true);
            inventory.setItem(slot, stack);
            event.getItem().remove();
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 2f);
            markDirtyNextTick(player);
            return;
        }
        // Backpack is full, so it lands on the hotbar and the ramp absorbs it
    }

    @UpdateEvent(delay = TICK_MILLIS)
    public void onUpdate() {
        OverloadedEffect.setSpeedReductionPerLevel(speedReductionPerLevel);

        for (Player player : Bukkit.getOnlinePlayers()) {
            final OverloadData playerData = data.computeIfAbsent(player.getUniqueId(), uuid -> new OverloadData());
            if (!enabled) {
                clear(player, playerData);
                continue;
            }

            tick(player, playerData);
        }
    }

    private void tick(Player player, OverloadData playerData) {
        if (playerData.dirty) {
            playerData.dirty = false;
            final int count = countWeapons(player);
            playerData.target = Math.min(maxLevel, Math.max(0, count - cap));

            if (playerData.snap) {
                playerData.snap = false;
                playerData.meter = playerData.target;
            } else if (count > playerData.lastCount && count > cap) {
                // Only when the hotbar gains one, so moving a weapon out never warns
                warn(player, count - cap);
            }

            playerData.lastCount = count;
        }

        if (playerData.meter < playerData.target) {
            final double step = riseSecondsPerLevel <= 0 ? playerData.target : (TICK_MILLIS / 1000d) / riseSecondsPerLevel;
            playerData.meter = Math.min(playerData.target, playerData.meter + step);
        } else if (playerData.meter > playerData.target) {
            final double step = fallSecondsPerLevel <= 0 ? playerData.meter : (TICK_MILLIS / 1000d) / fallSecondsPerLevel;
            playerData.meter = Math.max(playerData.target, playerData.meter - step);
        }

        applyLevel(player, playerData, (int) Math.floor(playerData.meter));
        updateActionBar(player, playerData);
    }

    private void applyLevel(Player player, OverloadData playerData, int level) {
        if (level == playerData.appliedLevel) {
            return;
        }

        playerData.appliedLevel = level;

        // Lower the pitch the deeper they are into the penalty
        final float pitch = (float) Math.max(0.5, 1.5 - (0.15 * level));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, pitch);

        if (level > 0) {
            effectManager.addEffect(player, null, EffectTypes.OVERLOADED, EFFECT_NAME, level, -1, true, true, false, null);
        } else {
            effectManager.removeEffect(player, EffectTypes.OVERLOADED, EFFECT_NAME, false);
        }
    }

    private void updateActionBar(Player player, OverloadData playerData) {
        final boolean show = actionBar && (playerData.target > 0 || playerData.meter > 0);
        if (show == (playerData.actionBarComponent != null)) {
            return;
        }

        final Client client = clientManager.search().online(player);
        if (client == null) {
            return; // Still logging in; the next tick will catch it
        }

        final Gamer gamer = client.getGamer();
        if (show) {
            playerData.actionBarComponent = new PermanentComponent(viewer -> actionBarText(playerData));
            gamer.getActionBar().add(ACTION_BAR_PRIORITY, playerData.actionBarComponent);
        } else {
            gamer.getActionBar().remove(playerData.actionBarComponent);
            playerData.actionBarComponent = null;
        }
    }

    private Component actionBarText(OverloadData playerData) {
        final TextColor highlight = TextColor.color(255, 69, 69);
        final Component text = playerData.appliedLevel > 0
                ? Translations.component("core.overload.actionbar.active",
                        Component.text(playerData.appliedLevel, highlight, TextDecoration.BOLD),
                        Component.text(playerData.target, highlight, TextDecoration.BOLD))
                : Translations.component("core.overload.actionbar.warning",
                        Component.text(playerData.target, highlight, TextDecoration.BOLD));

        return text.color(TextColor.color(255, 255, 255)).shadowColor(ShadowColor.shadowColor(0, 0, 0, 255));
    }

    private void warn(Player player, int over) {
        if (!warnMessage) {
            return;
        }

        UtilMessage.message(player, "core.prefix.overload", "core.overload.warning",
                Component.text(over, NamedTextColor.RED));
    }

    private void clear(Player player, OverloadData playerData) {
        playerData.meter = 0;
        playerData.target = 0;
        playerData.lastCount = 0;
        applyLevel(player, playerData, 0);
        updateActionBar(player, playerData);
    }

    private int countWeapons(Player player) {
        final PlayerInventory inventory = player.getInventory();
        final int limit = countInventory ? STORAGE_SLOTS : HOTBAR_SLOTS;

        int count = 0;
        for (int slot = 0; slot < limit; slot++) {
            if (counts(inventory.getItem(slot))) {
                count++;
            }
        }

        if (countArmor) {
            for (ItemStack armour : inventory.getArmorContents()) {
                if (counts(armour)) {
                    count++;
                }
            }
        }

        return count;
    }

    private boolean counts(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        return itemFactory.fromItemStack(itemStack)
                .filter(instance -> instance.getRarity().isAtLeast(getThreshold()))
                .filter(instance -> instance.getItemGroup() == ItemGroup.WEAPON
                        || (countArmor && instance.getItemGroup() == ItemGroup.ARMOR))
                .isPresent();
    }

    private ItemRarity getThreshold() {
        try {
            return ItemRarity.valueOf(rarityThreshold.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ItemRarity.LEGENDARY;
        }
    }

    private void markDirtyNextTick(Player player) {
        // These events fire before the inventory settles, so read it on the following tick
        UtilServer.runTaskLater(core, () -> {
            if (player.isOnline()) {
                data.computeIfAbsent(player.getUniqueId(), uuid -> new OverloadData()).dirty = true;
            }
        }, 1L);
    }

    private void snapNextTick(Player player) {
        UtilServer.runTaskLater(core, () -> {
            if (player.isOnline()) {
                final OverloadData playerData = data.computeIfAbsent(player.getUniqueId(), uuid -> new OverloadData());
                playerData.dirty = true;
                playerData.snap = true;
                playerData.appliedLevel = 0; // The entity was rebuilt, so nothing is applied to it any more
            }
        }, 1L);
    }

    private static final class OverloadData {
        private double meter;
        private int target;
        private int appliedLevel;
        private int lastCount;
        private boolean dirty = true;
        private boolean snap = true;
        private PermanentComponent actionBarComponent;
    }

}
