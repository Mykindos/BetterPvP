package me.mykindos.betterpvp.game.framework;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.event.GameChangeEvent;
import me.mykindos.betterpvp.game.framework.event.PreGameChangeEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameModule;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlag;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlagModule;
import me.mykindos.betterpvp.game.impl.domination.Domination;
import me.mykindos.betterpvp.game.impl.domination.DominationModule;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing games and their associated modules.
 */
@CustomLog
@Singleton
public class GameRegistry implements Listener {

    private final Map<AbstractGame<?, ?>, GameModule> registeredGames = Maps.newHashMap();
    private final Map<Class<? extends Listener>, Listener> activeListeners = Maps.newHashMap();

    private final GamePlugin plugin;
    private final Injector parentInjector;
    private final ServerController serverController;

    @Inject
    public GameRegistry(GamePlugin plugin, Injector parentInjector, ServerController serverController) {
        this.plugin = plugin;
        this.parentInjector = parentInjector;
        this.serverController = serverController;

        // Add handler for game scope management
        for (GameState state : GameState.values()) {
            serverController.getStateMachine().addEnterHandler(state, oldState -> handleStateChange(state, oldState));
        }

        // Games
        final CaptureTheFlag ctf = new CaptureTheFlag();
        registerGame(ctf, new CaptureTheFlagModule(ctf));
        final Domination dom = new Domination();
        registerGame(dom, new DominationModule(dom));

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Set<AbstractGame<?, ?>> getRegisteredGames() {
        return Collections.unmodifiableSet(registeredGames.keySet());
    }

    /**
     * Registers a game with the registry and its associated module
     *
     * @param game The game to register
     * @param module The module for the game
     */
    private void registerGame(AbstractGame<?, ?> game, GameModule module) {
        if (registeredGames.containsKey(game)) {
            throw new IllegalArgumentException("Game already registered: " + game.getClass().getSimpleName());
        }

        registeredGames.put(game, module);
        log.info("Registered game: {} with module: {}",
                game.getClass().getSimpleName(), module.getId()).submit();
    }

    /**
     * Unregisters a game from the registry
     *
     * @param game The game to unregister
     */
    private void unregisterGame(AbstractGame<?, ?> game) {
        final GameModule module = registeredGames.remove(game);
        if (module != null) {
            AbstractGame<?, ?> currentGame = serverController.getCurrentGame();
            // If this is the current active game, clean up its resources
            if (currentGame == game) {
                game.tearDown();
                unregisterModuleListeners(module);
                module.onDisable();
            }

            log.info("Unregistered game: {} with module: {}",
                    game.getClass().getSimpleName(), module.getId()).submit();
        }
    }

    /**
     * Handles game state changes
     *
     * @param newState The new game state
     * @param oldState The previous game state
     */
    private void handleStateChange(GameState newState, GameState oldState) {
        if (newState == GameState.IN_GAME && oldState != GameState.IN_GAME) {
            enterGame();
        }

        else if (oldState == GameState.IN_GAME && newState != GameState.IN_GAME) {
            exitGame();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreGameChange(PreGameChangeEvent event) {
        if (event.getOldGame() != null) {
            exitGameScope(); // Trashes the current game scope. Game is trashed when the state leaves IN_GAME, which is before this
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameChange(GameChangeEvent event) {
        if (event.getNewGame() != null) {
            enterGameScope(); // Only sets up the injector. Game is instantiated when the state is IN_GAME, which is after this
        }
    }

    private void enterGame() {
        AbstractGame<?, ?> currentGame = serverController.getCurrentGame();
        if (currentGame == null) {
            log.warn("Entered game scope but no game is set").submit();
            return;
        }

        GameModule module = registeredGames.get(currentGame);
        if (module == null) {
            log.warn("No module registered for game: {}", currentGame.getClass().getSimpleName()).submit();
            return;
        }

        // Initialize the module and game
        module.onEnable();
        currentGame.setup();

        // Register listeners using the game's injector
        registerModuleListeners(module, currentGame.injector);

        log.info("Game entered: {}",
                currentGame.getClass().getSimpleName()).submit();
    }

    /**
     * Activates the game scope and creates a child injector for the current game
     */
    private void enterGameScope() {
        AbstractGame<?, ?> currentGame = serverController.getCurrentGame();
        if (currentGame == null) {
            log.warn("Entered game scope but no game is set").submit();
            return;
        }

        GameModule module = registeredGames.get(currentGame);
        if (module == null) {
            log.warn("No module registered for game: {}", currentGame.getClass().getSimpleName()).submit();
            return;
        }

        // Enter the scope
        module.getScope().enter();

        // Create child injector and attach to game
        Injector gameInjector = parentInjector.createChildInjector(module);
        currentGame.bindInjector(gameInjector);

        log.info("Game scope activated with game: {} and module: {}",
                currentGame.getClass().getSimpleName(), module.getId()).submit();
    }

    /**
     * Deactivates the game scope and cleans up resources
     */
    private void exitGame() {
        AbstractGame<?, ?> currentGame = serverController.getCurrentGame();
        if (currentGame == null) {
            log.warn("Exited game scope but no game was set").submit();
            return;
        }

        // Unregister all active listeners
        for (Listener listener : activeListeners.values()) {
            HandlerList.unregisterAll(listener);
        }
        activeListeners.clear();

        // Disable the current game's module
        currentGame.tearDown();

        log.info("Game exited").submit();
    }

    private void exitGameScope() {
        AbstractGame<?, ?> currentGame = serverController.getCurrentGame();
        if (currentGame == null) {
            log.warn("Exited game scope but no game was set").submit();
            return;
        }

        GameModule module = registeredGames.get(currentGame);
        if (module != null) {
            module.onDisable();
            module.getScope().exit();
        }

        log.info("Exited game scope").submit();
    }

    /**
     * Registers all listeners for a module using the provided injector
     *
     * @param module The module containing listener classes
     * @param injector The injector to create listener instances
     */
    private void registerModuleListeners(GameModule module, Injector injector) {
        for (Class<? extends Listener> listenerClass : module.getListeners()) {
            try {
                // Use the game's injector to create listener instances
                Listener listener = injector.getInstance(listenerClass);
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                activeListeners.put(listenerClass, listener);
                log.info("Registered listener: {}", listenerClass.getSimpleName()).submit();
            } catch (Exception e) {
                log.error("Failed to register listener: " + listenerClass.getSimpleName(), e).submit();
            }
        }
    }

    /**
     * Unregisters all listeners for a module
     *
     * @param module The module
     */
    private void unregisterModuleListeners(GameModule module) {
        for (Class<? extends Listener> listenerClass : module.getListeners()) {
            Listener listener = activeListeners.remove(listenerClass);
            if (listener != null) {
                HandlerList.unregisterAll(listener);
                log.info("Unregistered listener: {}", listenerClass.getSimpleName()).submit();
            }
        }
    }

    /**
     * Gets the current injector based on the active game
     *
     * @return The current game's injector or the parent injector if no game is active
     */
    public Injector getCurrentInjector() {
        AbstractGame<?, ?> currentGame = serverController.getCurrentGame();
        return (currentGame != null && currentGame.injector != null)
                ? currentGame.injector
                : parentInjector;
    }

    /**
     * Gets the module for a game
     *
     * @param game The game
     * @return The game's module, or null if not found
     */
    public GameModule getModule(AbstractGame<?, ?> game) {
        return registeredGames.get(game);
    }

}