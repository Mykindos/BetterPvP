package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.listener.player.event.ParticipantPreRespawnEvent;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@GameScoped
public class SuddenDeathListener implements Listener {

    private final GameController gameController;
    private final PlayerController playerController;

    @Inject
    public SuddenDeathListener(GameController gameController, PlayerController playerController) {
        this.gameController = gameController;
        this.playerController = playerController;
    }

    @EventHandler
    public void onPlayerRespawn(ParticipantPreRespawnEvent event) {
        if (gameController.isSuddenDeath()) {
            event.setCancelled(true); // No respawning in sudden death
        }
    }

    @EventHandler
    public void onPlayerStopSpectate(ParticipantStopSpectatingEvent event) {
        if (gameController.isSuddenDeath()) {
            //kill the player if it is sudden death
            UtilServer.runTaskLater(JavaPlugin.getPlugin(GamePlugin.class), () -> {
                playerController.setAlive(event.getPlayer(), event.getParticipant(), false);
            }, 1);
        }
    }
}
