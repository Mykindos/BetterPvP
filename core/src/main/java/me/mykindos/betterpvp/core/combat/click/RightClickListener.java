package me.mykindos.betterpvp.core.combat.click;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEndEvent;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

@BPvPListener
@Singleton
@CustomLog
public class RightClickListener implements Listener {

    /** Grace period (ms) before evicting a canBlock player with a cosmetic shield after release. */
    private static final long COSMETIC_SHIELD_RELEASE_GRACE_MS = 150;

    private final ClientManager clientManager;
    private final WeakHashMap<Player, RightClickContext> rightClickCache = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> lastDrop = new WeakHashMap<>();
    /**
     * Tracks the first moment we observed !(isHandRaised || isBlocking || hasActiveItem) for a
     * canBlock player.  We only evict once that condition has persisted for > 100 ms, which
     * avoids false-positives from the 1-2 tick delay between startUsingItem() and the server
     * reflecting the item-use state.  The timer is reset every time onRightClick fires (i.e.
     * every time the client re-sends the right-click packet while holding), so the effective
     * window is "100ms since the last client right-click packet", giving ~100ms release lag
     * while still being well above the ~50ms observed client re-send interval.
     */
    private final WeakHashMap<Player, Long> suspectedRelease = new WeakHashMap<>();

    @Inject
    public RightClickListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    // Fix for interact event triggering when dropping items
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        lastDrop.put(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if(!event.getAction().isLeftClick()) return;
        if (this.lastDrop.containsKey(event.getPlayer()) && System.currentTimeMillis() - this.lastDrop.get(event.getPlayer()) <= 50L) {
            event.setCancelled(true);
            this.lastDrop.remove(event.getPlayer());
        }
    }

    @UpdateEvent
    public void onUpdate() {
        // Refund items from offhand to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand.getType() != Material.AIR && !UtilItem.isUndroppable(offhand)) {
                ItemStack temp = player.getInventory().getItemInOffHand().clone();
                player.getInventory().setItemInOffHand(null);
                UtilItem.insert(player, temp);
            }
        }

        // Update the right click cache
        final Iterator<Map.Entry<Player, RightClickContext>> iterator = rightClickCache.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, RightClickContext> entry = iterator.next();
            final Player player = entry.getKey();
            final RightClickContext context = entry.getValue();
            final ItemStack previouslyHolding = context.getItemStack();
            final Gamer gamer = context.getGamer();
            if (!player.isOnline()) {
                iterator.remove();
                gamer.setLastBlock(-1);
                suspectedRelease.remove(player);
                if (UtilItem.isUndroppable(player.getInventory().getItemInOffHand())) {
                    player.getInventory().setItemInOffHand(null);
                }
                continue;
            }

            // If the click took longer than 250ms, remove it from the cache
            // Unless they're blocking with a shield, meaning they are still holding right click
            final boolean canBlock = gamer.canBlock();
            if (!canBlock && System.currentTimeMillis() - context.getTime() > 249) {
                log.debug("[RCL] Evicting {} — no-block timeout ({}ms since context)", player.getName(),
                        System.currentTimeMillis() - context.getTime()).submit();
                iterator.remove();
                gamer.setLastBlock(-1);
                suspectedRelease.remove(player);
                final RightClickEndEvent releaseEvent = new RightClickEndEvent(context.getGamer().getPlayer());
                UtilServer.callEvent(releaseEvent);
                continue;
            }

            if (canBlock) {
                final boolean stillUsing = player.isHandRaised() || player.isBlocking() || player.hasActiveItem();
                if (stillUsing) {
                    // Player is actively using the item — clear any pending release timer
                    suspectedRelease.remove(player);
                } else {
                    // The grace period is only needed when a cosmetic shield was placed via
                    // startUsingItem(OFF_HAND), which causes isHandRaised/hasActiveItem to be
                    // unreliable for 1-2 ticks. If no undroppable shield is in the offhand,
                    // no startUsingItem was called and we can evict immediately.
                    final boolean hasCosmicShield = UtilItem.isUndroppable(player.getInventory().getItemInOffHand());
                    if (!hasCosmicShield) {
                        log.debug("[RCL] Evicting {} — canBlock release (no cosmetic shield, immediate)", player.getName()).submit();
                        suspectedRelease.remove(player);
                        iterator.remove();
                        gamer.setLastBlock(-1);
                        final RightClickEndEvent releaseEvent = new RightClickEndEvent(context.getGamer().getPlayer());
                        UtilServer.callEvent(releaseEvent);
                        continue;
                    }

                    // Cosmetic shield path — use the grace timer to absorb startUsingItem() latency
                    final long suspectedAt = suspectedRelease.computeIfAbsent(player, p -> System.currentTimeMillis());
                    final long suspectedFor = System.currentTimeMillis() - suspectedAt;
                    log.debug("[RCL] {} canBlock=true (cosmetic shield) but not using — suspectedFor={}ms | raised={} blocking={} activeItem={}",
                            player.getName(), suspectedFor,
                            player.isHandRaised(), player.isBlocking(), player.hasActiveItem()).submit();
                    if (suspectedFor > COSMETIC_SHIELD_RELEASE_GRACE_MS) {
                        log.debug("[RCL] Evicting {} — canBlock release confirmed after {}ms", player.getName(), suspectedFor).submit();
                        suspectedRelease.remove(player);
                        iterator.remove();
                        gamer.setLastBlock(-1);
                        final RightClickEndEvent releaseEvent = new RightClickEndEvent(context.getGamer().getPlayer());
                        UtilServer.callEvent(releaseEvent);
                        continue;
                    }
                }
            }

            // Keep holding state by slot + item type only (ignore durability/meta fluctuations).
            final EquipmentSlot cachedSlot = context.getEvent().getHand();
            final ItemStack holding = player.getInventory().getItem(cachedSlot);
            if (cachedSlot != EquipmentSlot.HAND || holding.getType() != previouslyHolding.getType()) {
                log.debug("[RCL] Evicting {} — item type mismatch (was={} now={})", player.getName(),
                        previouslyHolding.getType(), holding.getType()).submit();
                iterator.remove();
                gamer.setLastBlock(-1);
                suspectedRelease.remove(player);
                final RightClickEndEvent releaseEvent = new RightClickEndEvent(context.getGamer().getPlayer());
                UtilServer.callEvent(releaseEvent);
                continue;
            }

            // Otherwise, keep holding the item
            gamer.setLastBlock(System.currentTimeMillis());
            final RightClickEvent previousEvent = context.getEvent();
            final RightClickEvent event = new RightClickEvent(player,
                    null,
                    true,
                    previousEvent.getHand());
            UtilServer.callEvent(event);
        }
    }

    // Handle right click events
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent event) {
        // Cancel if they do another type of click that's not right-click
        if (event.getAction().isLeftClick()) {
            final Player player = event.getPlayer();
            final Gamer gamer = this.clientManager.search().online(player).getGamer();
            final ItemStack main = player.getInventory().getItemInMainHand();
            final ItemStack off = player.getInventory().getItemInOffHand();
            final boolean sword = UtilItem.isSword(main) || UtilItem.isSword(off);
            // Ignore temporary undroppable offhand shields so channel skills do not get force-released on left click.
            final boolean shield = main.getType().equals(Material.SHIELD)
                    || (off.getType().equals(Material.SHIELD) && !UtilItem.isUndroppable(off));
            if (!rightClickCache.containsKey(player) || !(sword || shield)) {
                return;
            }

            rightClickCache.remove(player);
            gamer.setLastBlock(-1);
            suspectedRelease.remove(player);
            if (UtilItem.isUndroppable(off)) {
                player.getInventory().setItemInOffHand(null);
            }
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        //noinspection deprecation
        if (clickedBlock != null && (clickedBlock.getType().isInteractable() && !clickedBlock.getType().name().contains("STAIR"))
                || event.getAction() == Action.PHYSICAL
                || event.getHand() != EquipmentSlot.HAND) {
            return; // Return if they are not right-clicking or if they are right-clicking a usable block
        }

        // Call event
        final Player player = event.getPlayer();
        // Use the actual mainhand item as fallback instead of AIR — when startUsingItem(OFF_HAND)
        // fires a synthetic PlayerInteractEvent, event.getItem() is null but the player's mainhand
        // hasn't changed. Storing AIR would cause a type-mismatch eviction on the next tick.
        final ItemStack item = Objects.requireNonNullElse(event.getItem(), player.getInventory().getItemInMainHand());
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.setLastBlock(System.currentTimeMillis());
        final RightClickEvent clickEvent = new RightClickEvent(player, null, false, event.getHand());
        final RightClickContext context = new RightClickContext(gamer, clickEvent, item);
        final RightClickContext previous = rightClickCache.remove(player);
        if (previous != null) {
            clickEvent.setHoldClick(true);
        }

        UtilServer.callEvent(clickEvent);
        rightClickCache.put(player, context);
        // Reset the canBlock release timer — the client just re-sent its right-click, so it is
        // definitely still holding.  This makes suspectedRelease track "time since last packet"
        // rather than "time since first !stillUsing tick", keeping the release detection tight.
        suspectedRelease.remove(player);
    }

    @EventHandler
    public void onPickupShield(EntityPickupItemEvent event) {
        if (UtilItem.isUndroppable(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onClickOffhand(InventoryClickEvent event) {
        // Prevent from touching the shield
        if (event.getClickedInventory() != null) {
            if (event.getCurrentItem() != null) {
                if (UtilItem.isUndroppable(event.getCurrentItem())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReleaseClick(RightClickEndEvent event) {
        Player player = event.getPlayer();
        if (UtilItem.isUndroppable(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinalShieldCheck(RightClickEvent event) {
        final Player player = event.getPlayer();
        final ItemStack offhand = player.getInventory().getItemInOffHand();

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Remove shield if we are not blocking
        if (!event.hasBlockingItem() || event.getBlockingItem().getType().isAir()) {
            if (UtilItem.isUndroppable(offhand)) {
                player.getInventory().setItemInOffHand(null);
            }
            return;
        }

        if (UtilItem.isUndroppable(offhand)) {
            return; // Don't replace if we are already holding a cosmetic shield
        }

        // Replace offhand with shield because we are blocking
        player.getInventory().setItemInOffHand(UtilItem.makeUndroppable(event.getBlockingItem()));
        player.startUsingItem(EquipmentSlot.OFF_HAND);
    }

    @EventHandler
    public void onDropOffhand(PlayerDropItemEvent event) {
        final ItemStack item = event.getItemDrop().getItemStack();
        if (UtilItem.isUndroppable(item)) {
            event.setCancelled(true);
        }
    }

}
