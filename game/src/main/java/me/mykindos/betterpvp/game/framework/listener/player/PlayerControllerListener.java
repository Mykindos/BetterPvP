package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.event.ChampionsBuildLoadedEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerCombatLogEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@BPvPListener
@Singleton
public class PlayerControllerListener implements Listener {

    private final PlayerController playerController;
    private final ClientManager clientManager;
    private final HotBarLayoutManager layoutManager;

    @Inject
    public PlayerControllerListener(PlayerController playerController, ClientManager clientManager, HotBarLayoutManager layoutManager) {
        this.playerController = playerController;
        this.clientManager = clientManager;
        this.layoutManager = layoutManager;
    }

    @EventHandler
    public void onBuildLoad(ChampionsBuildLoadedEvent event) {
        layoutManager.load(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuilderDelete(DeleteBuildEvent event) {
        layoutManager.resetLayout(event.getPlayer(), event.getRoleBuild());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());
        playerController.registerPlayer(event.getPlayer(), new Participant(event.getPlayer(), client));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        playerController.unregisterPlayer(event.getPlayer());
        layoutManager.getHotBarLayouts().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCombatLog(PlayerCombatLogEvent event) {
        event.setSafe(true); // Stop spawning combat loggers
    }

}
