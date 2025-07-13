package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class ParticipantStateListener implements Listener {

    private final PlayerController playerController;
    private final ServerController serverController;

    @Inject
    public ParticipantStateListener(PlayerController playerController, ServerController serverController) {
        this.playerController = playerController;
        this.serverController = serverController;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        // When game ends, set all participants to ALIVE
        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> {
            playerController.getEverybody().forEach((player, participant) ->  {
                playerController.setAlive(player, participant, true);

                // Toggle spectating if they are spectating and don't want to spectate next game
                // this is because people can log on mid-game and not want to spectate the next
                if (participant.isSpectating() && !participant.isSpectateNextGame()) {
                    playerController.setSpectating(player, participant, false, true);
                }
            });
        });
    }

    // Sets people to dead when they die
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player player)) {
            return;
        }

        if (serverController.getCurrentState() == GameState.IN_GAME) {
            Participant participant = playerController.getParticipant(player);
            playerController.setAlive(player, participant, false);
        }
    }

    // Set people to spectator when they join the game after it has started
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(ClientJoinEvent event) {
        final Participant participant = playerController.getParticipant(event.getPlayer());
        if(participant == null) {
            log.warn("Null participant, somehow...").submit();
        }
        if (serverController.getCurrentState() == GameState.IN_GAME || serverController.getCurrentState() == GameState.ENDING) {
            playerController.setSpectating(event.getPlayer(), participant, true, false);
        }
    }

}
