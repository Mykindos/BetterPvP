package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.listener.player.event.ParticipantPreRespawnEvent;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@GameScoped
public class SuddenDeathListener implements Listener {

    private final GameController gameController;

    @Inject
    public SuddenDeathListener(GameController gameController) {
        this.gameController = gameController;
    }

    @EventHandler
    public void onPlayerRespawn(ParticipantPreRespawnEvent event) {
        if (gameController.isSuddenDeath()) {
            event.setCancelled(true); // No respawning in sudden death
        }
    }
}
