package me.mykindos.betterpvp.game.framework.listener.team;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@BPvPListener
@Singleton
public class TeamJoinQuitListener implements Listener {

    private final ServerController controller;
    private final PlayerController playerController;

    @Inject
    public TeamJoinQuitListener(ServerController controller, PlayerController playerController) {
        this.controller = controller;
        this.playerController = playerController;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        final AbstractGame<?, ?> game = controller.getCurrentGame();
        if (!(game instanceof TeamGame<?> teamGame)) {
            return;
        }

        // Remove them from their team
        final Participant participant = playerController.getParticipant(event.getPlayer());
        teamGame.removePlayerFromTeam(participant);

        // Check for win conditions when players leave during an active game
        if (controller.getCurrentState() == GameState.IN_GAME && game.attemptGracefulEnding()) {
            controller.transitionTo(GameState.ENDING);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStartSpectate(ParticipantStartSpectatingEvent event) {
        final AbstractGame<?, ?> game = controller.getCurrentGame();
        if (!(game instanceof TeamGame<?> teamGame)) {
            return;
        }

        // Remove them from their team
        final Participant participant = playerController.getParticipant(event.getPlayer());
        teamGame.removePlayerFromTeam(participant);
    }
}
