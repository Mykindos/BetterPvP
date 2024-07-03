package me.mykindos.betterpvp.core.combat.click;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
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
public class RightClickListener implements Listener {

    private final ClientManager clientManager;
    private final Core core;
    private final WeakHashMap<Player, RightClickContext> rightClickCache = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> lastDrop = new WeakHashMap<>();

    @Inject
    public RightClickListener(ClientManager clientManager, Core core) {
        this.clientManager = clientManager;
        this.core = core;
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
            if (offhand.getType() != Material.AIR && !UtilItem.isCosmeticShield(offhand)) {
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
                if (UtilItem.isCosmeticShield(player.getInventory().getItemInOffHand())) {
                    player.getInventory().setItemInOffHand(null);
                }
                continue;
            }

            // If the click took longer than 250ms, remove it from the cache
            // Unless they're blocking with a shield, meaning they are still holding right click
            if (!(gamer.canBlock() && (player.isBlocking() || player.isHandRaised())) && System.currentTimeMillis() - context.getTime() > 249) {
                iterator.remove();
                gamer.setLastBlock(-1);
                final RightClickEndEvent releaseEvent = new RightClickEndEvent(context.getGamer().getPlayer());
                UtilServer.callEvent(releaseEvent);
                continue;
            }

            // If they aren't holding the same item anymore, remove it from the cache
            // and call the release of the right click
            ItemStack holding = player.getInventory().getItem(context.getEvent().getHand());
            if (!UtilItem.isSimilar(holding, previouslyHolding)) {
                iterator.remove();
                gamer.setLastBlock(-1);
                final RightClickEndEvent releaseEvent = new RightClickEndEvent(context.getGamer().getPlayer());
                UtilServer.callEvent(releaseEvent);
                continue;
            }

            // Otherwise, keep holding the item
            gamer.setLastBlock(System.currentTimeMillis());
            final RightClickEvent previousEvent = context.getEvent();
            final RightClickEvent event = new RightClickEvent(player,
                    previousEvent.isUseShield(),
                    previousEvent.getShieldModelData(),
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
            final boolean shield = main.getType().equals(Material.SHIELD) || off.getType().equals(Material.SHIELD);
            if (!rightClickCache.containsKey(player) || !(sword || shield)) {
                return;
            }

            rightClickCache.remove(player);
            gamer.setLastBlock(-1);
            if (UtilItem.isCosmeticShield(off)) {
                player.getInventory().setItemInOffHand(null);
            }
            return;
        }

        if (event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()
                || event.getAction() == Action.PHYSICAL
                || event.getHand() != EquipmentSlot.HAND) {
            return; // Return if they are not right-clicking or if they are right-clicking a usable block
        }

        // Check for default blocking
        final ItemStack item = Objects.requireNonNullElse(event.getItem(), new ItemStack(Material.AIR));

        // Call event
        final Player player = event.getPlayer();
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.setLastBlock(System.currentTimeMillis());
        final RightClickEvent clickEvent = new RightClickEvent(player, false, 0, false, event.getHand());
        final RightClickContext context = new RightClickContext(gamer, clickEvent, item);
        final RightClickContext previous = rightClickCache.remove(player);
        if (previous != null) {
            clickEvent.setHoldClick(true);
            clickEvent.setUseShield(previous.getEvent().isUseShield());
            clickEvent.setShieldModelData(previous.getEvent().getShieldModelData());
        }

        UtilServer.callEvent(clickEvent);
        rightClickCache.put(player, context);
    }

    @EventHandler
    public void onPickupShield(EntityPickupItemEvent event) {
        if (UtilItem.isCosmeticShield(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onClickOffhand(InventoryClickEvent event) {
        // Prevent from touching the shield
        if (event.getClickedInventory() != null) {
            if (event.getCurrentItem() != null) {
                if (UtilItem.isCosmeticShield(event.getCurrentItem())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReleaseClick(RightClickEndEvent event) {
        Player player = event.getPlayer();
        if (UtilItem.isCosmeticShield(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinalShieldCheck(RightClickEvent event) {
        if (!event.isHoldClick() || event.getHand() != EquipmentSlot.HAND) {
            return; // Only update at the start of the click
        }

        Player player = event.getPlayer();

        // Remove shield if we are not blocking
        final ItemStack offhand = player.getInventory().getItemInOffHand();
        if (!event.isUseShield()) {
            if (offhand.getType() == Material.SHIELD) {
                player.getInventory().setItemInOffHand(null);
            }
            return;
        }

        if (UtilItem.isCosmeticShield(offhand)) {
            return; // Don't replace if we are already holding a cosmetic shield
        }

        // Replace offhand with shield because we are blocking
        player.getInventory().setItemInOffHand(UtilItem.createCosmeticShield(event.getShieldModelData()));
    }

    @EventHandler
    public void onDropOffhand(PlayerDropItemEvent event) {
        final ItemStack item = event.getItemDrop().getItemStack();
        if (UtilItem.isCosmeticShield(item)) {
            event.setCancelled(true);
        }
    }

}
