package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.transport.ClanTravelHubMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;

@BPvPListener
@Singleton
public class VoidWorldListener implements Listener {

    private final ClientManager clientManager;
    private final ClanManager clanManager;
    private final World voidWorld;

    @Inject
    public VoidWorldListener(ClientManager clientManager, ClanManager clanManager) {
        this.clientManager = clientManager;
        this.clanManager = clanManager;
        voidWorld = new BPvPWorld(BPvPWorld.VOID_WORLD_NAME).getWorld();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (event.getDamagee().getWorld().getName().equals(BPvPWorld.VOID_WORLD_NAME)) {
            event.cancel("Cannot take damage in void world");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);
        Gamer gamer = client.getGamer();

        Optional<String> property = gamer.getProperty(GamerProperty.PREFERRED_SPAWN);
        if (!player.hasPlayedBefore() || property.isEmpty() || property.get().isEmpty()) {
            player.teleport(voidWorld.getSpawnLocation());
            player.setGameMode(GameMode.ADVENTURE);
            new ClanTravelHubMenu(player, client, clanManager).show(player);
        }
    }

    @UpdateEvent(delay = 500)
    public void checkTransportMenu() {
        for (Player player : voidWorld.getPlayers()) {
            if (!player.isOp()) {
                if (player.getOpenInventory().getType() == InventoryType.CRAFTING) {
                    new ClanTravelHubMenu(player, clientManager.search().online(player), clanManager).show(player);
                }

                if (player.getGameMode() != GameMode.ADVENTURE) {
                    player.setGameMode(GameMode.ADVENTURE);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);
        Gamer gamer = client.getGamer();

        Optional<String> property = gamer.getProperty(GamerProperty.PREFERRED_SPAWN);
        if (property.isEmpty() || property.get().isEmpty()) {
            return;
        }

        Optional<Clan> preferredSpawnOptional = clanManager.getClanByName(property.get());
        if (preferredSpawnOptional.isEmpty()) {
            return;
        }

        Clan clan = preferredSpawnOptional.get();
        if (clan.getCore() == null || clan.getCore().getPosition() == null) {
            return;
        }

        event.setRespawnLocation(clan.getCore().getPosition());
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(player.getWorld().getName().equals(BPvPWorld.VOID_WORLD_NAME)) {
            if(!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

}
