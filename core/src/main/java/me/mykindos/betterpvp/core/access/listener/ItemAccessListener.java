package me.mykindos.betterpvp.core.access.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.access.AccessRequirement;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessService;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gates USE, DAMAGE, and WEAR scopes via cancellation + action-bar denial messages.
 *
 * <p>CRAFT scope is handled at the recipe level ({@link me.mykindos.betterpvp.core.recipe.Recipe#canCraft})
 * and does not need a listener here.</p>
 *
 * <p>USE is detected via {@link PlayerInteractEvent} before the interaction system fires — we hook at
 * LOW priority so any cancellation from here stops the chain at HIGHEST in InteractionListener.</p>
 */
@BPvPListener
@Singleton
public class ItemAccessListener implements Listener {

    /** Milliseconds between repeated denial notifications for the same (player, item) pair. */
    private static final long DENIAL_FEEDBACK_THROTTLE_MS = 1500L;

    private final ItemAccessService itemAccessService;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;

    // Presence-only cache: any entry inside the TTL means "still throttled". Caffeine evicts
    // entries automatically after the throttle window, so the map cannot grow without bound
    // even for players who quit or items that are never seen again.
    private final Cache<ThrottleKey, Boolean> denialThrottle = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(DENIAL_FEEDBACK_THROTTLE_MS))
            .build();

    @Inject
    public ItemAccessListener(ItemAccessService itemAccessService, ItemFactory itemFactory, ItemRegistry itemRegistry) {
        this.itemAccessService = itemAccessService;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    private record ThrottleKey(UUID playerId, Key itemKey) { }

    // USE scope

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack itemStack = player.getEquipment().getItemInMainHand();
        if (itemStack.getType().isAir()) return;

        checkAndDeny(player, itemStack, AccessScope.USE, cancelled -> {
            if (cancelled) event.setCancelled(true);
        });
    }

    // DAMAGE scope

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack itemStack = player.getEquipment().getItemInMainHand();
        if (itemStack.getType().isAir()) return;

        checkAndDeny(player, itemStack, AccessScope.DAMAGE, cancelled -> {
            if (cancelled) event.setCancelled(true);
        });
    }

    // WEAR scope

    // PlayerArmorChangeEvent is not cancellable — we undo equip by updating inventory server-side.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItem();
        if (newItem == null || newItem.getType().isAir()) return;

        checkAndDeny(player, newItem, AccessScope.WEAR, cancelled -> {
            if (cancelled) {
                // Undo the equip by reverting the armour slot and updating the client inventory.
                // We remove the newly equipped item and restore the previous one.
                org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                PlayerArmorChangeEvent.SlotType slot = event.getSlotType();
                ItemStack oldItem = event.getOldItem();

                switch (slot) {
                    case HEAD -> inv.setHelmet(oldItem);
                    case CHEST -> inv.setChestplate(oldItem);
                    case LEGS -> inv.setLeggings(oldItem);
                    case FEET -> inv.setBoots(oldItem);
                }

                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Core.class), player::updateInventory);
            }
        });
    }

    private void checkAndDeny(Player player, ItemStack itemStack, AccessScope scope, Consumer<Boolean> cancelAction) {
        Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(itemStack);
        if (instanceOpt.isEmpty()) return;

        ItemInstance instance = instanceOpt.get();
        BaseItem baseItem = instance.getBaseItem();
        NamespacedKey nsk = itemRegistry.getKey(baseItem);
        if (nsk == null) return;
        Key itemKey = Key.key(nsk.namespace(), nsk.getKey());

        Optional<AccessRequirement> blocker = itemAccessService.firstBlocker(player, baseItem, itemKey, scope);
        if (blocker.isEmpty()) return;

        cancelAction.accept(true);
        denyAndNotify(player, itemKey, blocker.get());
    }

    private void denyAndNotify(Player player, Key itemKey, AccessRequirement requirement) {
        if (isThrottled(player.getUniqueId(), itemKey)) return;

        player.sendActionBar(requirement.lore().colorIfAbsent(NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
    }

    private boolean isThrottled(UUID playerId, Key itemKey) {
        ThrottleKey key = new ThrottleKey(playerId, itemKey);
        if (denialThrottle.getIfPresent(key) != null) {
            return true;
        }
        denialThrottle.put(key, Boolean.TRUE);
        return false;
    }
}
