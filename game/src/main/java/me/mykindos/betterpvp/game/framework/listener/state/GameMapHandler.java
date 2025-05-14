package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.GameRegistry;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.attribute.global.CurrentMapAttribute;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles game map management, including loading, selecting, and switching game maps
 * based on the current game state. This class integrates with the server state machine
 * to perform appropriate actions when state transitions occur. It also ensures that the
 * waiting lobby and game maps are loaded and functioning properly.
 *
 * Responsibilities include:
 * - Managing the waiting lobby world and ensuring it is loaded correctly.
 * - Selecting a random game and associated map for the server.
 * - Handling game state-specific transitions related to maps and worlds.
 * - Registering and responding to map-related attribute changes.
 * - Listening to server events such as weather changes to apply game-specific logic.
 *
 * Annotations:
 * - {@code @Singleton}: Indicates that this class should follow the singleton pattern.
 * - {@code @BPvPListener}: Marks this class as a listener specific to certain events or logic in BPvP.
 * - {@code @CustomLog}: Enables custom logging capabilities for this handler.
 */
@Singleton
@BPvPListener
@CustomLog
public class GameMapHandler implements Listener {

    private final GameRegistry gameRegistry;
    private final ServerController controller;
    private final MapManager mapManager;
    private final MappedWorld waitingLobby;
    private final CurrentMapAttribute currentMapAttribute;

    @Inject
    public GameMapHandler(GameRegistry gameRegistry, ServerController controller, MapManager mapManager,
                          @Named("Waiting Lobby") MappedWorld waitingLobby, CurrentMapAttribute currentMapAttribute) {
        this.gameRegistry = gameRegistry;
        this.controller = controller;
        this.mapManager = mapManager;
        this.waitingLobby = waitingLobby;
        this.currentMapAttribute = currentMapAttribute;
        setupStateHandlers();
    }

    /**
     * Sets up state handlers responsible for handling transitions and events
     * related to the game states and world management.
     *
     * This method initializes the world and configures behavior for specific
     * state transitions within the game framework, ensuring proper world
     * creation, selection, and cleanup during transitions.
     *
     * Key functionality included:
     * - Invokes the creation of the game world during server startup.
     * - Adds an enter handler for the `WAITING` state to handle world loading
     *   and random game/map selection unless transitioning from the `STARTING` state.
     * - Configures a listener on the `currentMapAttribute` to handle unloading of
     *   the previous map and loading of the new map when the map changes.
     */
    private void setupStateHandlers() {
        // Create the world
        createWorld(); // Default when server starts
        controller.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> {
            if (oldState != GameState.STARTING) { // Just so we can swap between WAITING and STARTING without changing the map
                waitingLobby.createWorld(); // Merely loads the world
            }
        });

        // Select a random game when transitioning to WAITING state
        controller.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> {
            if (oldState != GameState.STARTING) { // Just so we can swap between WAITING and STARTING without changing the game/map
                selectRandomGameAndMap();
            }
        });

        currentMapAttribute.addChangeListener((old, newMap) -> {
            if (old != null) {
                old.unloadWorld(); // unload previous
            }
            if (newMap != null) {
                newMap.createWorld();
            }
        });
    }

    /**
     * Selects a random game and associated map for the server to use.
     *
     * This method performs the following actions:
     * 1. Retrieves the set of registered games from the game registry.
     * 2. If no games are registered, logs an error message and shuts down the server.
     * 3. If only one game is registered, it selects that game and its associated map.
     * 4. If multiple games are registered, a random game is chosen, excluding the current game (if applicable).
     *    If all games are excluded after filtering, the full set of registered games is used instead.
     * 5. Updates the server's current game to the selected game.
     * 6. Invokes another method to select and load a random map for the chosen game.
     *
     * This method ensures that a new game and map are set up for gameplay while avoiding re-selection of the
     * currently active game if possible.
     */
    public void selectRandomGameAndMap() {
        // Select a random game
        final Set<AbstractGame<?, ?>> games = gameRegistry.getRegisteredGames();
        if (games.isEmpty()) {
            log.error("No games registered, shutting down...").submit();
            Bukkit.shutdown();
            return;
        }

        // If there's only one game, we have no choice but to use it
        if (games.size() == 1) {
            AbstractGame<?, ?> game = games.iterator().next();
            controller.setGame(game);
            selectMapForGame(game);
            return;
        }

        // Get current game to avoid selecting it again
        AbstractGame<?, ?> currentGame = controller.getCurrentGame();

        // Create a list of games excluding the current one
        List<AbstractGame<?, ?>> availableGames = games.stream()
                .filter(game -> currentGame == null || !game.getClass().equals(currentGame.getClass()))
                .toList();

        // If for some reason we filtered out all games, use the full set
        if (availableGames.isEmpty()) {
            availableGames = new ArrayList<>(games);
        }

        // Select a random game from available options
        AbstractGame<?, ?> game = availableGames.get(ThreadLocalRandom.current().nextInt(availableGames.size()));
        controller.setGame(game);

        // Select a random map for this game
        selectMapForGame(game);
    }

    /**
     * Selects a map for the specified game. If a map is already loaded, it will be unloaded before the new map is set.
     * This method selects a random map that matches the game's configuration.
     *
     * @param game the game for which a map needs to be selected
     */
    private void selectMapForGame(AbstractGame<?, ?> game) {
        // Select a random map for this game
        Optional<MappedWorld> randomMapOpt = mapManager.selectRandomMap(game);
        if (randomMapOpt.isEmpty()) {
            log.error("No maps available for game {}!", game.getConfiguration().getName()).submit();
            return;
        }

        final MappedWorld currentMap = mapManager.getCurrentMap();
        if (currentMap != null) {
            currentMap.unloadWorld(); // unload previous
        }

        MappedWorld gameMap = randomMapOpt.get();
        mapManager.setCurrentMap(gameMap);
    }



    /**
     * Creates and initializes the waiting lobby world if it is not already loaded.
     * If the world fails to load, logs an error and shuts down the server.
     *
     * The method first checks if the `waitingLobby` is loaded. If not, it attempts to
     * load the world using the `createWorld` method of the `waitingLobby` instance.
     * After attempting to load, the method rechecks the loaded status. If the world
     * still fails to load, an error is logged, and the server shuts down.
     *
     * This method ensures that the waiting lobby world is prepared for use and
     * provides a fallback mechanism to terminate the server in case of critical
     * initialization failure.
     */
    private void createWorld() {
        // Load lobby world if not already loaded
        if (!waitingLobby.isLoaded()) {
            waitingLobby.createWorld(); // Merely loads the world
        }

        if (!waitingLobby.isLoaded()) {
            log.error("Failed to load waiting lobby, shutting down...").submit();
            Bukkit.shutdown();
        }
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        event.setCancelled(true);
    }
}