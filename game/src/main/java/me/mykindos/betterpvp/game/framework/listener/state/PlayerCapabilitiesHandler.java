package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class PlayerCapabilitiesHandler implements Listener {

    private final PlayerController playerController;
    private final ServerController serverController;

    @Inject
    public PlayerCapabilitiesHandler(PlayerController playerController, ServerController serverController) {
        this.playerController = playerController;
        this.serverController = serverController;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        serverController.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> updatePlayerCapabilities());
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> updatePlayerCapabilities());
    }

    private void updatePlayerCapabilities() {
        playerController.getEverybody().forEach(playerController::updatePlayerCapabilities);
    }
}
