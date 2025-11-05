package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanRelationshipEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanTerritoryEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.clans.clans.map.ClanMapService;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@BPvPListener
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MapListener implements Listener {

    private final Clans clans;
    private final CooldownManager cooldownManager;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final ClanMapService clanMapService;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        clanMapService.removePlayerMapData(player);

        // Remove filled maps from player inventory
        for (ItemStack value : player.getInventory().all(Material.FILLED_MAP).values()) {
            player.getInventory().remove(value);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        clanMapService.loadChunks(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.FILLED_MAP);
    }

    @EventHandler
    public void onSpawn(PlayerRespawnEvent event) {
        Boolean keepInventory = event.getPlayer().getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
        if (keepInventory == Boolean.FALSE) {
            ItemStack mapItem = clanMapService.createMapItem();
            event.getPlayer().getInventory().setItem(8, itemFactory.convertItemStack(mapItem).orElse(mapItem));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftMap(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.getType() == Material.MAP) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropMap(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.FILLED_MAP) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClanAlly(ClanRelationshipEvent event) {
        UtilServer.runTaskLater(clans, () -> {
            for (ClanMember member : event.getClan().getMembers()) {
                clanMapService.updateClanChunks(UUID.fromString(member.getUuid()));
            }

            for (ClanMember member : event.getTargetClan().getMembers()) {
                clanMapService.updateClanChunks(UUID.fromString(member.getUuid()));
            }
        }, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClaim(ClanTerritoryEvent event) {
        if (event.isCancelled()) return;
        clanMapService.updateClaims(event.getClan());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClanLeave(MemberLeaveClanEvent event) {
        if (event.isCancelled()) return;

        UtilServer.runTaskLater(clans, () -> {
            clanMapService.resetPlayerClanColors(event.getPlayer());
        }, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisband(ClanDisbandEvent event) {
        if (event.isCancelled()) return;

        UtilServer.runTaskLater(clans, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                clanMapService.removeClanFromPlayerMap(online, event.getClan());
                clanMapService.updatePlayerClanRelations(online);
                clanMapService.updateStatus(online);
            }
        }, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinClan(MemberJoinClanEvent event) {
        if (event.isCancelled()) return;

        UtilServer.runTaskLater(clans, () -> {
            clanMapService.updatePlayerClanColors(event.getPlayer(), event.getClan());
            clanMapService.updateStatus(event.getPlayer());
        }, 1);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        final Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) {
            return;
        }

        if (!(event.getAction().name().contains("RIGHT") || event.getAction().name().contains("LEFT"))) {
            return;
        }

        if (!cooldownManager.use(player, "Map Zoom", 0.1, false, false)) {
            return;
        }

        final Client client = clientManager.search().online(player);
        final MapSettings mapSettings = clanMapService.getOrCreateMapSettings(player);

        MapSettings.Scale scale = null;

        // Handle zoom in/out
        if (event.getAction().name().contains("RIGHT")) {
            // Zoom out
            MapSettings.Scale curScale = mapSettings.getScale();

            if (curScale == MapSettings.Scale.FAR && !client.isAdministrating()) {
                return;
            } else if (curScale == MapSettings.Scale.FARTHEST) {
                return;
            }

            scale = mapSettings.setScale(MapSettings.Scale.values()[curScale.ordinal() + 1]);
        } else if (event.getAction().name().contains("LEFT")) {
            // Zoom in
            MapSettings.Scale curScale = mapSettings.getScale();

            if (curScale == MapSettings.Scale.CLOSEST) {
                return;
            }

            scale = mapSettings.setScale(MapSettings.Scale.values()[curScale.ordinal() - 1]);
        }

        if (scale != null) {
            final MapSettings.Scale finalScale = scale;
            client.getGamer().getActionBar().add(500, new TimedComponent(1.5, false,
                    gmr -> createZoomBar(finalScale)));
            mapSettings.setUpdate(true);
        }
    }

    private Component createZoomBar(MapSettings.Scale scale) {
        return Component.text("Zoom: ", NamedTextColor.WHITE)
                .append(Component.text((scale.getValue()) + "x", NamedTextColor.GREEN));
    }
}
