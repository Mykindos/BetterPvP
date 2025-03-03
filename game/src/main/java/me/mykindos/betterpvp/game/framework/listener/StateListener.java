package me.mykindos.betterpvp.game.framework.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.framework.state.GameStateMachine;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles default state transitions and events.
 * <p>
 * Global scope, its lifecycle is that of the server.
 */
public class StateListener implements Listener {

    private final JavaPlugin plugin;
    private final ServerController controller;
    private BukkitTask currentTask;

    @Inject
    public StateListener(JavaPlugin plugin, ServerController controller) {
        this.plugin = plugin;
        this.controller = controller;

        // Setup state handlers
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        GameStateMachine stateMachine = controller.getStateMachine();

        // Register handlers for state transitions
        stateMachine.addEnterHandler(GameState.STARTING, oldState -> startGameCountdown());
        stateMachine.addEnterHandler(GameState.ENDING, oldState -> startEndingCountdown());
        stateMachine.addEnterHandler(GameState.WAITING, oldState -> checkStateRequirements());

    }

    private void startGameCountdown() {
        startCountdown(30, GameState.IN_GAME);
    }

    private void startEndingCountdown() {
        startCountdown(10, GameState.WAITING);
    }

    private void startCountdown(int seconds, GameState targetState) {
        cancelCurrentTask();

        final int[] timeLeft = {seconds};
        currentTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            timeLeft[0]--;

            if (timeLeft[0] <= 0) {
                cancelCurrentTask();
                controller.transitionTo(targetState);
            }
        }, 20L, 20L); // Run every second
    }

    public void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    public void checkStateRequirements() {
        final AbstractGame<?> game = controller.getCurrentGame();
        switch (controller.getCurrentState()) {
            case STARTING: {
                if (game == null) {
                    cancelCurrentTask();
                    return;
                }

                int requiredPlayers = game.getConfiguration().getRequiredPlayers();
                if (controller.getPlayerCount() < requiredPlayers) {
                    // Not enough players, cancel countdown and return to waiting
                    cancelCurrentTask();
                    controller.transitionTo(GameState.WAITING);
                }
                break;
            }
            case WAITING: {
                if (game == null) {
                    return;
                }

                int requiredPlayers = game.getConfiguration().getRequiredPlayers();
                if (controller.getPlayerCount() >= requiredPlayers) {
                    // Enough players, start countdown
                    controller.transitionTo(GameState.STARTING);
                }
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        controller.registerPlayer(event.getPlayer().getUniqueId());
        checkStateRequirements();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        controller.unregisterPlayer(event.getPlayer().getUniqueId());
        checkStateRequirements();
    }
}