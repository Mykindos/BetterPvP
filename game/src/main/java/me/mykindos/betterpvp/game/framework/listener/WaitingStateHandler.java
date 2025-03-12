package me.mykindos.betterpvp.game.framework.listener;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import dev.brauw.mapper.region.PerspectiveRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.GameRegistry;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.manager.TeamSelectorManager;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles player teleportation during the WAITING state and game map selection
 */
@Singleton
@BPvPListener
@CustomLog
public class WaitingStateHandler implements Listener {

    private final GameRegistry gameRegistry;
    private final ServerController controller;
    private final MapManager mapManager;
    private final MappedWorld waitingLobby;

    private final PerspectiveRegion spawnPoint;

    @Inject
    public WaitingStateHandler(GameRegistry gameRegistry, ServerController controller, MapManager mapManager,
                               @Named("Waiting Lobby") MappedWorld waitingLobby) {
        this.gameRegistry = gameRegistry;
        this.controller = controller;
        this.mapManager = mapManager;
        this.waitingLobby = waitingLobby;
        this.spawnPoint = waitingLobby.findRegion("spawnpoint", PerspectiveRegion.class).findFirst()
                .orElseThrow(() -> new IllegalStateException("No spawnpoint found in waiting lobby"));

        setupStateHandlers();

        // Bind waiting lobby to Guice
        bindWaitingLobby();
    }

    /**
     * Binds the waiting lobby to Guice for injection
     */
    private void bindWaitingLobby() {
        gameRegistry.getCurrentInjector().createChildInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MappedWorld.class).annotatedWith(Names.named("waitingLobby")).toInstance(waitingLobby);
            }
        });
    }

    private void setupStateHandlers() {
        // Teleport players to lobby when transitioning to WAITING state
        controller.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> {
            if (oldState == GameState.STARTING) {
                return; // Return because this has already been handled, we're just transitioning back to waiting
            }

            // Load lobby world if not already loaded
            if (!waitingLobby.isLoaded()) {
                waitingLobby.createWorld(); // Merely loads the world
            }

            if (!waitingLobby.isLoaded()) {
                log.error("Failed to load waiting lobby, shutting down...").submit();
                Bukkit.shutdown();
                return;
            }

            // Schedule teleport to run after any world changes are complete
            teleportAllPlayersToLobby();
        });

        // Select a random game when transitioning to WAITING state
        controller.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> {
            // Select a random game
            final Set<AbstractGame<?>> games = gameRegistry.getRegisteredGames();
            final Optional<AbstractGame<?>> gameOpt = games.stream()
                    .skip(ThreadLocalRandom.current().nextInt(games.size()))
                    .findAny();
            if (gameOpt.isEmpty()) {
                log.error("No games registered, shutting down...").submit();
                Bukkit.shutdown();
                return;
            }

            AbstractGame<?> game = gameOpt.get();
            controller.setGame(game);

            // Select a random map for this game
            Optional<MappedWorld> randomMapOpt = mapManager.selectRandomMap();
            if (randomMapOpt.isEmpty()) {
                log.error("No maps available for game {}!", game.getConfiguration().getName()).submit();
                return;
            }

            final MappedWorld currentMap = mapManager.getCurrentMap();
            if (currentMap != null) {
                currentMap.unloadWorld(); // unload previous
            }

            MappedWorld gameMap = randomMapOpt.get();
            gameMap.createWorld(); // load it up
            mapManager.setCurrentMap(gameMap);

            log.info("Selected map {} for game {}", gameMap.getName(), game.getConfiguration().getName()).submit();
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (controller.getCurrentState() == GameState.WAITING) {
            teleportToLobby(event.getPlayer());
        }
    }

    /**
     * Teleports all online players to the lobby
     */
    public void teleportAllPlayersToLobby() {
        if (waitingLobby.isLoaded()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                teleportToLobby(player);
            }
            log.info("Teleported all players to lobby").submit();
        }
    }

    /**
     * Teleports a player to the lobby
     * @param player The player to teleport
     */
    public boolean teleportToLobby(Player player) {
        if (waitingLobby.isLoaded()) {
            player.teleport(spawnPoint.getLocation());
            return true;
        }
        return false;
    }
}