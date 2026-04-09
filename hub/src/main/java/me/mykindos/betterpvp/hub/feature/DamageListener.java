package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.feature.zone.Zone;
import me.mykindos.betterpvp.hub.feature.zone.ZoneService;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

@BPvPListener
@Singleton
public class DamageListener implements Listener {

    private final HubWorld hubWorld;
    private final ZoneService zoneService;

    @Inject
    private DamageListener(HubWorld hubWorld, ZoneService zoneService) {
        this.hubWorld = hubWorld;
        this.zoneService = zoneService;
    }

    // Disable PvP outside FFA
    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.getDamagee() instanceof Player player && zoneService.getZone(player) != Zone.FFA) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Player player && zoneService.getZone(player) != Zone.FFA) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (zoneService.getZone(event.getPlayer()) != Zone.NONE) {
            event.getPlayer().getInventory().clear();
            UtilServer.runTaskLater(JavaPlugin.getPlugin(Hub.class), () -> {
                event.getPlayer().spigot().respawn();
            }, 3L);
        }
    }

    // this gets overwritten by FFA for ffa players
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        event.getPlayer().getInventory().clear();
        event.setRespawnLocation(hubWorld.getSpawnpoint().getLocation());
    }


}
