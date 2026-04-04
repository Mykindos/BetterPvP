package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.feature.menu.ServerSelectorMenu;
import me.mykindos.betterpvp.hub.feature.zone.Zone;
import me.mykindos.betterpvp.hub.feature.zone.ZoneService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;

@BPvPListener
@Singleton
public class HotbarListener implements Listener {

    private final NetworkPlayerCountService networkPlayerCountService;
    private final HubInventoryService inventoryService;
    private final ZoneService zoneService;

    @Inject
    public HotbarListener(NetworkPlayerCountService networkPlayerCountService, HubInventoryService inventoryService,
                          ZoneService zoneService) {
        this.networkPlayerCountService = networkPlayerCountService;
        this.inventoryService = inventoryService;
        this.zoneService = zoneService;
    }

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        inventoryService.applyHubHotbar(Objects.requireNonNull(event.getClient().getGamer().getPlayer()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (GameMode.CREATIVE.equals(event.getWhoClicked().getGameMode())) {
            return;
        }

        if (event.getClickedInventory() instanceof PlayerInventory
                && zoneService.getZone((Player) event.getWhoClicked()) != Zone.FFA) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (GameMode.CREATIVE.equals(event.getPlayer().getGameMode())) {
            return;
        }
        if (zoneService.getZone(event.getPlayer()) != Zone.FFA) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }

        final ItemStack item = Objects.requireNonNull(event.getItem());
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return;
        }

        final int customModelData = item.getItemMeta().getCustomModelData();
        switch (customModelData) {
            case 700:
                new ServerSelectorMenu(networkPlayerCountService).show(event.getPlayer());
                break;
        }
    }


}
