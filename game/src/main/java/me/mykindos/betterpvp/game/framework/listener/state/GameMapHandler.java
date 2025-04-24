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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles player teleportation during the WAITING state and game map selection
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

    public void selectRandomGameAndMap() {
        // Select a random game
        final Set<AbstractGame<?, ?>> games = gameRegistry.getRegisteredGames();
        if (games.isEmpty()) {
            log.error("No games registered, shutting down...").submit();
            Bukkit.shutdown();
            return;
        }

        final Optional<AbstractGame<?, ?>> gameOpt = games.stream()
                .skip(ThreadLocalRandom.current().nextInt(games.size()))
                .findAny();
        if (gameOpt.isEmpty()) {
            log.error("No games registered, shutting down...").submit();
            Bukkit.shutdown();
            return;
        }

        AbstractGame<?, ?> game = gameOpt.get();
        controller.setGame(game);

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