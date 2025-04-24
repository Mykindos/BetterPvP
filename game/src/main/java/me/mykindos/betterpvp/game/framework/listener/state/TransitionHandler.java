package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.event.GameChangeEvent;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.GameDurationAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.RequiredPlayersAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.global.StartPausedAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.global.StartingCountdownAttribute;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.framework.state.GameStateMachine;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles default state transitions and events.
 * <p>
 * Global scope, its lifecycle is that of the server.
 */
@BPvPListener
@Singleton
public class TransitionHandler implements Listener {

    private final GamePlugin plugin;
    private final ServerController controller;
    private final PlayerController playerController;
    private final StartingCountdownAttribute startingCountdownAttribute;
    private final StartPausedAttribute startPausedAttribute;
    
    private Timer currentTask;
    @Getter
    private long estimatedTransitionTime = 0;

    @Inject
    public TransitionHandler(GamePlugin plugin, ServerController controller, PlayerController playerController,
                             StartingCountdownAttribute startingCountdownAttribute, StartPausedAttribute startPausedAttribute) {
        this.controller = controller;
        this.plugin = plugin;
        this.playerController = playerController;
        this.startingCountdownAttribute = startingCountdownAttribute;
        this.startPausedAttribute = startPausedAttribute;

        // Setup state handlers
        setupStateHandlers();

        Bukkit.getPluginManager().registerEvents(this, plugin); // early registration is needed for initial GameChangeEvent
    }

    private void setupStateHandlers() {
        GameStateMachine stateMachine = controller.getStateMachine();

        // Register handlers for state transitions
        stateMachine.addEnterHandler(GameState.WAITING, oldState -> {
            cancelCurrentTask();
            checkStateRequirements();
        });
        stateMachine.addEnterHandler(GameState.STARTING, oldState -> startGameCountdown());
        stateMachine.addEnterHandler(GameState.IN_GAME, oldState -> startGameTimer());
        stateMachine.addEnterHandler(GameState.ENDING, oldState -> startEndingCountdown());

        // Handle start pauses
        startPausedAttribute.addChangeListener((old, value) -> {
            // If the game is paused, cancel the current task
            if (value) {
                cancelCurrentTask();
                if (controller.getCurrentState() != GameState.WAITING) {
                    controller.transitionTo(GameState.WAITING);
                }
            }

            // If the game is unpaused, check if we need to start a countdown
            else {
                checkStateRequirements();
            }
        });

        // Handle countdown changes
        startingCountdownAttribute.addChangeListener((old, value) -> {
            if (controller.getCurrentState() == GameState.STARTING) {
                cancelCurrentTask();
                startGameCountdown();
            }
        });
    }

    private void startGameTimer() {
        final AbstractGame<?, ?> game = controller.getCurrentGame();
        Preconditions.checkState(game != null, "Game is null");
        final GameDurationAttribute gameDurationAttribute = game.getAttribute(GameDurationAttribute.class);
        startCountdown(gameDurationAttribute.getValue(), GameState.ENDING, false);
    }

    private void startGameCountdown() {
        startCountdown(startingCountdownAttribute.getValue(), GameState.IN_GAME, true);
    }

    private void startEndingCountdown() {
        startCountdown(Duration.ofSeconds(7L),  GameState.WAITING, false);
    }

    private void startCountdown(Duration duration, GameState targetState, boolean isTicking) {
        cancelCurrentTask();

        estimatedTransitionTime = System.currentTimeMillis() + duration.toMillis();
        currentTask = new Timer();
        final long durationSeconds = duration.toSeconds();

        if (isTicking) {
            new SoundEffect(Sound.UI_BUTTON_CLICK, 1f, 1f).broadcast();
        }

        currentTask.schedule(new TimerTask() {
            long ticks = 0;
            @Override
            public void run() {
                ticks += 1;

                if (ticks / 20 >= durationSeconds) {
                    currentTask = null;
                    UtilServer.runTask(plugin, () -> controller.transitionTo(targetState));
                    cancel();
                    return;
                }

                if (isTicking && durationSeconds - ticks / 20 <= 11 && ticks % 20 == 0) {
                    final float volume = (ticks / 20f) / durationSeconds;
                    final float pitch = 2f * ((ticks / 20f) / durationSeconds);
                    new SoundEffect(Sound.BLOCK_NOTE_BLOCK_PLING, pitch, volume).broadcast();
                }
            }
        }, 0, 50);
    }

    public void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    public boolean checkStateRequirements(boolean bypassPause) {
        final AbstractGame<?, ?> game = controller.getCurrentGame();
        switch (controller.getCurrentState()) {
            case STARTING: {
                if (game == null) {
                    cancelCurrentTask();
                    return false;
                }

                int requiredPlayers = game.getAttribute(RequiredPlayersAttribute.class).getValue();
                if (playerController.getParticipants().size() < requiredPlayers) {
                    // Not enough players, cancel countdown and return to waiting
                    cancelCurrentTask();
                    controller.transitionTo(GameState.WAITING);
                    return false;
                }

                return true;
            }
            case WAITING: {
                if (game == null) {
                    return false;
                }

                int requiredPlayers = game.getAttribute(RequiredPlayersAttribute.class).getValue();
                if ((bypassPause || !startPausedAttribute.getValue()) && playerController.getParticipants().size() >= requiredPlayers) {
                    // Enough players and start isnt paused, start countdown
                    controller.transitionTo(GameState.STARTING);
                    return true;
                }
                break;
            }
        }

        return false;
    }

    public boolean checkStateRequirements() {
        return checkStateRequirements(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkStateRequirements();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        checkStateRequirements();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStartSpectate(ParticipantStartSpectatingEvent event) {
        checkStateRequirements();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStopSpectate(ParticipantStopSpectatingEvent event) {
        checkStateRequirements();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGame(GameChangeEvent event) {
        if (event.getNewGame() != null) {
            event.getNewGame().getAttribute(RequiredPlayersAttribute.class).addChangeListener((old, newValue) -> checkStateRequirements());
        }
    }
}
