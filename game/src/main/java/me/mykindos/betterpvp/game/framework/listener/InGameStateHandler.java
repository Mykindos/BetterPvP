package me.mykindos.betterpvp.game.framework.listener;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.util.BukkitTaskScheduler;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Handles player teleportation and game start countdown when in IN_GAME state
 */
@BPvPListener
@CustomLog
@Singleton
public class InGameStateHandler implements Listener {

    private static final double COUNTDOWN_DURATION = 10.0; // Countdown duration in seconds

    private final ClientManager clientManager;
    private final ServerController controller;
    private final BukkitTaskScheduler scheduler;
    private final MapManager mapManager;

    private BukkitTask countdownTask;
    private final AtomicDouble countdownSeconds = new AtomicDouble(0);

    private final PermanentComponent actionBar = new PermanentComponent(gmr -> {
        if (!isCountdownActive()) {
            return null;
        }

        final double remaining = countdownSeconds.get();
        final ProgressBar progressBar = ProgressBar.withProgress((float) (remaining / COUNTDOWN_DURATION)).inverted();
        return Component.text("Game Start").appendSpace()
                .append(progressBar.build())
                .append(Component.text(" " + String.format("%.1f", remaining) + "s"));
    });

    @Inject
    public InGameStateHandler(ClientManager clientManager, ServerController controller, BukkitTaskScheduler scheduler, MapManager mapManager) {
        this.clientManager = clientManager;
        this.controller = controller;
        this.scheduler = scheduler;
        this.mapManager = mapManager;
        setupStateHandlers();
    }

    public boolean isCountdownActive() {
        return controller.getStateMachine().getCurrentState() == GameState.IN_GAME && countdownTask != null;
    }

    private void setupStateHandlers() {
        // When transitioning to IN_GAME state
        controller.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            // Don't allow TeamGame to start if any player doesn't have a team
            if (controller.getCurrentGame() instanceof TeamGame teamGame) {
                List<Player> playersWithoutTeam = new ArrayList<>();

                for (Player player : controller.getParticipants()) {
                    if (teamGame.getPlayerTeam(player) == null) {
                        playersWithoutTeam.add(player);
                    }
                }

                if (!playersWithoutTeam.isEmpty()) {
                    // Cancel transition to IN_GAME
                    log.warn("Cannot start game - {} players don't have a team", playersWithoutTeam.size()).submit();

                    // Notify players
                    for (Player player : controller.getParticipants()) {
                        if (playersWithoutTeam.contains(player)) {
                            UtilMessage.simpleMessage(player, "<red>You need to select a team to play!");
                            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
                        } else {
                            UtilMessage.simpleMessage(player, "<red>Waiting for all players to select a team...");
                        }
                    }

                    // Return to STARTING state
                    controller.transitionTo(GameState.WAITING);
                    return;
                }
            }

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

            // Teleport players to spawnpoints and start countdown
            teleportPlayersToSpawnpoints(gameWorld);
            startGameCountdown();
        });

        // When exiting IN_GAME state, cancel any active countdown
        controller.getStateMachine().addExitHandler(GameState.IN_GAME, newState -> {
            cancelCountdown();
        });
    }

    /**
     * Teleports all players to available spawnpoints.
     * For team games, players are teleported to their team's spawnpoints.
     */
    private void teleportPlayersToSpawnpoints(MappedWorld gameWorld) {
        Set<Player> players = new HashSet<>(controller.getParticipants());

        if (controller.getCurrentGame() instanceof TeamGame teamGame) {
            // Handle team-based teleportation
            for (Team team : teamGame.getTeams().values()) {
                // Find team-specific spawnpoints
                List<PerspectiveRegion> teamSpawnpoints = gameWorld
                        .findRegion("spawnpoint_" + team.getProperties().name().toLowerCase(), PerspectiveRegion.class)
                        .toList();

                if (teamSpawnpoints.isEmpty()) {
                    log.warn("No spawnpoints found for team {}", team.getProperties().name()).submit();
                    final Location defaultLocation = new Location(Objects.requireNonNull(gameWorld.getWorld()), 0, 100, 0);
                    teamSpawnpoints = List.of(new PerspectiveRegion("generic", defaultLocation));
                }

                // Teleport team members to their spawnpoints
                int spawnIndex = 0;
                for (Player player : new ArrayList<>(team.getPlayers())) {
                    PerspectiveRegion spawnpoint = teamSpawnpoints.get(spawnIndex % teamSpawnpoints.size());
                    player.teleport(spawnpoint.getLocation());
                    players.remove(player);
                    spawnIndex++;
                }
            }
        }

        // Handle remaining/non-team players - use generic spawnpoints
        if (!players.isEmpty()) {
            List<PerspectiveRegion> spawnpoints = gameWorld.findRegion("spawnpoint", PerspectiveRegion.class).toList();
            if (spawnpoints.isEmpty()) {
                log.warn("No generic spawnpoints found in map").submit();
                return;
            }

            int spawnIndex = 0;
            for (Player player : players) {
                PerspectiveRegion spawnpoint = spawnpoints.get(spawnIndex % spawnpoints.size());
                player.teleport(spawnpoint.getLocation());
                spawnIndex++;
            }
        }
    }

    /**
     * Starts the game countdown
     */
    private void startGameCountdown() {
        cancelCountdown(); // Cancel any existing countdown
        countdownSeconds.set(COUNTDOWN_DURATION);

        clientManager.getOnline().forEach(client -> client.getGamer().getActionBar().add(0, actionBar));

        // Start countdown task - runs every tick (1/20 second)
        countdownTask = scheduler.scheduleRecurringTask(() -> {
            double remaining = countdownSeconds.getAndAdd(-0.05);

            if (remaining <= 0) {
                endCountdown();
            }
        }, 1, 1); // Every tick (20 ticks = 1 second)
    }

    /**
     * Ends the countdown and unfreezes players
     */
    private void endCountdown() {
        cancelCountdown();
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
        if (!isCountdownActive() || !controller.getParticipants().contains(event.getPlayer())) {
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
        if (!controller.getParticipants().contains(event.getPlayer()) || controller.getStateMachine().getCurrentState() != GameState.IN_GAME) {
            event.setCancelled(true); // Cancel skill use if not in IN_GAME state
        }
    }
}