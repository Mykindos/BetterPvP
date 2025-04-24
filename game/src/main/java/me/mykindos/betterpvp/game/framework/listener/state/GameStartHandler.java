package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles player teleportation and game start countdown when in IN_GAME state
 */
@BPvPListener
@CustomLog
@Singleton
public class GameStartHandler implements Listener {

    private static final double COUNTDOWN_DURATION = 10.0; // Countdown duration in seconds

    private final GamePlugin plugin;
    private final ClientManager clientManager;
    private final ServerController controller;
    private final MapManager mapManager;
    private final PlayerController playerController;

    private BukkitTask countdownTask;
    private final AtomicInteger countdownTicks = new AtomicInteger(0);

    private final PermanentComponent actionBar = new PermanentComponent(gmr -> {
        if (!isCountdownActive()) {
            return null;
        }

        final double remaining = countdownTicks.get() / 20d;
        final ProgressBar progressBar = ProgressBar.withProgress((float) (remaining / COUNTDOWN_DURATION)).inverted();
        return Component.text("Game Start").appendSpace()
                .append(progressBar.build())
                .append(Component.text(" " + String.format("%.1f", remaining) + "s"));
    });

    @Inject
    public GameStartHandler(GamePlugin plugin, ClientManager clientManager, ServerController controller, MapManager mapManager, PlayerController playerController) {
        this.plugin = plugin;
        this.clientManager = clientManager;
        this.controller = controller;
        this.mapManager = mapManager;
        this.playerController = playerController;
        setupStateHandlers();
    }

    public boolean isCountdownActive() {
        return controller.getStateMachine().getCurrentState() == GameState.IN_GAME && countdownTask != null;
    }

    private void setupStateHandlers() {
        // When transitioning to IN_GAME state
        controller.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            // Get the map that was selected during game initialization
            MappedWorld gameWorld = mapManager.getCurrentMap();

            if (gameWorld == null) {
                log.error("No map selected for game, cannot start!").submit();
                controller.transitionTo(GameState.WAITING);
                return;
            }

            // Make sure the world is loaded
            if (!gameWorld.isLoaded()) {
                gameWorld.createWorld();
            }

            if (!gameWorld.isLoaded()) {
                log.error("Failed to load map {}, cannot start game!", gameWorld.getName()).submit();
                controller.transitionTo(GameState.WAITING);
                return;
            }

            // Announce game start and setup
            final AbstractGame<?, ?> game = controller.getCurrentGame();
            announceStart(game);

            // Start countdown
            startGameCountdown();
        });

        // When exiting IN_GAME state, cancel any active countdown
        controller.getStateMachine().addExitHandler(GameState.IN_GAME, newState -> {
            cancelCountdown();
        });
    }

    private void announceStart(AbstractGame<?, ?> game) {
        final MappedWorld map = mapManager.getCurrentMap();
        final String mapName = map.getMetadata().getName();
        final String author = "BetterPvP Build Team";

        Bukkit.broadcast(Component.text(" ".repeat(50), NamedTextColor.GREEN, TextDecoration.STRIKETHROUGH));
        Bukkit.broadcast(Component.text("Game - ", NamedTextColor.GREEN)
                .append(Component.text(game.getConfiguration().getName(), NamedTextColor.YELLOW, TextDecoration.BOLD)));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(game.getDescription());
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Map - ", NamedTextColor.GREEN)
                .append(Component.text(mapName, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" created by ", NamedTextColor.GRAY))
                .append(Component.text(author, NamedTextColor.YELLOW, TextDecoration.BOLD)));
        Bukkit.broadcast(Component.text(" ".repeat(50), NamedTextColor.GREEN, TextDecoration.STRIKETHROUGH));
    }

    /**
     * Starts the game countdown
     */
    private void startGameCountdown() {
        cancelCountdown(); // Cancel any existing countdown
        final int startTicks = (int) (COUNTDOWN_DURATION * 20);
        countdownTicks.set(startTicks); // Set initial countdown value

        // Add the action bar
        clientManager.getOnline().forEach(client -> client.getGamer().getActionBar().add(3, actionBar));

        // Start the task
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                int remainingTicks = countdownTicks.addAndGet(-1);

                // Play TP sound
                if (remainingTicks == startTicks - 5) {
                    new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f).broadcast();
                }

                // Play countdown sound
                else if (remainingTicks == 3 * 20) {
                    new SoundEffect("betterpvp", "game.countdown").broadcast();
                }

                if (remainingTicks <= 0) {
                    showGameStart();
                    cancelCountdown();
                }
            }
        }.runTaskTimer(plugin, 0, 1); // Every tick (20 ticks = 1 second)
    }

    private void showGameStart() {
        new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 2f, 2f).broadcast();
        for (Client client : clientManager.getOnline()) {
            final Gamer gamer = client.getGamer();
            gamer.getActionBar().add(2, new TimedComponent(3.0, false, gmr -> {
                return Component.text("GAME STARTED!", NamedTextColor.GREEN, TextDecoration.BOLD);
            }));
        }
    }

    /**
     * Cancels the active countdown task
     */
    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        clientManager.getOnline().forEach(client -> client.getGamer().getActionBar().remove(actionBar));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isCountdownActive()) {
            return;
        }

        final Participant participant = playerController.getParticipant(event.getPlayer());
        if (!participant.isAlive()) {
            return;
        }

        // Allow looking around, but not movement
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamage(CustomDamageEvent event) {
        if (isCountdownActive()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSkillUse(PlayerUseSkillEvent event) {
        if (isCountdownActive()) {
            event.setCancelled(true); // Cancel skill use if not in IN_GAME state
        }
    }
}