package me.mykindos.betterpvp.game.mineplex;

import com.google.inject.Inject;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.game.*;
import com.mineplex.studio.sdk.modules.game.event.PostMineplexGameStateChangeEvent;
import com.mineplex.studio.sdk.modules.game.event.PreMineplexGameStateChangeEvent;
import com.mineplex.studio.sdk.modules.queuing.QueuingModule;
import lombok.NonNull;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.event.AcceptingPlayersStateEvent;
import me.mykindos.betterpvp.game.framework.event.GameStateChangeEvent;
import me.mykindos.betterpvp.game.framework.event.PreGameStateChangeEvent;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public class ChampionsGame implements MineplexGame, Listener {

    private final ServerController serverController;
    private final PlayerController playerController;
    private boolean requeue = false;

    @Inject
    private ChampionsGame(GamePlugin plugin, ServerController serverController, PlayerController playerController) {
        this.serverController = serverController;
        this.playerController = playerController;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        final MineplexGameModule gameModule = MineplexModuleManager.getRegisteredModule(MineplexGameModule.class);
        gameModule.setCurrentGame(this);
    }

    @Override
    public @NonNull String getName() {
        return "Champions";
    }

    @Override
    public @NonNull MineplexGameMechanicFactory getGameMechanicFactory() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public @NonNull GameState getGameState() {
        return getGameState(serverController.isAcceptingPlayers(), serverController.getCurrentState());
    }

    private GameState getGameState(boolean acceptingPlayers, me.mykindos.betterpvp.game.framework.state.GameState gameState) {
        if (requeue) {
            return BuiltInGameState.ENDED;
        }

        if (!acceptingPlayers) {
            return BuiltInGameState.PREPARING;
        }

        return switch (gameState) {
            case WAITING, STARTING -> BuiltInGameState.PRE_START;
            case IN_GAME -> BuiltInGameState.STARTED;
            case ENDING -> BuiltInGameState.ENDED;
        };
    }

    @Override
    public void setGameState(@NonNull GameState gameState) {
        throw new UnsupportedOperationException("Game state changes are not supported through ChampionsGame. Use ServerController instead.");
    }

    @Override
    public @NonNull PlayerState getPlayerState(@NonNull Player player) {
        final Participant participant = Objects.requireNonNull(playerController.getParticipant(player));
        if (playerController.getParticipants().containsKey(player)) {
            if (participant.isAlive()) {
                return BuiltInPlayerState.ALIVE;
            } else if (serverController.getCurrentGame().getConfiguration().getRespawnsAttribute().getValue()) {
                return BuiltInPlayerState.RESPAWNING;
            } else if (participant.isSpectating()) {
                return BuiltInPlayerState.ELIMINATED;
            }
        }

        return BuiltInPlayerState.SPECTATOR;
    }

    @Override
    public void setup() {
//        serverController.getStateMachine().addEnterHandler(me.mykindos.betterpvp.game.framework.state.GameState.WAITING, old -> {
//            // If the game ended, and only if
//            if (old != me.mykindos.betterpvp.game.framework.state.GameState.ENDING) {
//                return;
//            }
//
//            // Set the game to null
//            requeue = true;
//            serverController.setGame(null);
//            serverController.setAcceptingPlayers(false);
//
//            // Requeue all players
//            final QueuingModule queuingModule = MineplexModuleManager.getRegisteredModule(QueuingModule.class);
//            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
//                queuingModule.requeuePlayer(onlinePlayer).thenAccept(unused -> {
//                    UtilMessage.message(onlinePlayer, "Game", "Requeuing you to another game...");
//                }).exceptionally(throwable -> {
//                    UtilMessage.message(onlinePlayer, "Game", "<red>Failed to requeue for another game.");
//                    return null;
//                });
//            }
//        });
    }

    @Override
    public void teardown() {
        // ignore
    }

    @EventHandler
    public void onPreStateChange(PreGameStateChangeEvent event) {
        final GameState oldState = getGameState(serverController.isAcceptingPlayers(), event.getOldState());
        final GameState newState = getGameState(serverController.isAcceptingPlayers(), event.getNewState());
        new PreMineplexGameStateChangeEvent(this, oldState, newState).callEvent();
    }

    @EventHandler
    public void onStateChange(GameStateChangeEvent event) {
        final GameState oldState = getGameState(serverController.isAcceptingPlayers(), event.getOldState());
        final GameState newState = getGameState(serverController.isAcceptingPlayers(), event.getNewState());
        new PostMineplexGameStateChangeEvent(this, oldState, newState).callEvent();
    }

    @EventHandler
    public void onAccepting(AcceptingPlayersStateEvent event) {
        final GameState old = getGameState(!event.isAcceptingPlayers(), serverController.getCurrentState());
        final GameState current = getGameState(event.isAcceptingPlayers(), serverController.getCurrentState());
        new PreMineplexGameStateChangeEvent(this, old, current).callEvent();
        new PostMineplexGameStateChangeEvent(this, old, current).callEvent();
    }
}
