package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@BPvPListener
@Singleton
public class JoinQuitListener implements Listener {

    @Inject
    private HubWorld hubWorld;
    @Inject
    private CombatFeaturesService combatFeaturesService;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        combatFeaturesService.setActive(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(ClientJoinEvent event) {
        event.setJoinMessage(null);
        combatFeaturesService.setActive(event.getPlayer(), false);
        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().setTotalExperience(0);
        event.getPlayer().setExperienceLevelAndProgress(0);
        if (event.getPlayer().getAttribute(Attribute.MAX_HEALTH) != null) {
            event.getPlayer().setHealth(event.getPlayer().getAttribute(Attribute.MAX_HEALTH).getValue());
        }

        event.getPlayer().teleport(hubWorld.getSpawnpoint().getLocation());
    }

    @EventHandler
    public void onQuit(ClientQuitEvent event) {
        combatFeaturesService.clear(event.getPlayer());
        event.setQuitMessage(null);
    }

}
