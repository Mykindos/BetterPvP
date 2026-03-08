package me.mykindos.betterpvp.game.impl.domination.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.domination.Domination;
import me.mykindos.betterpvp.game.impl.domination.controller.GameController;
import me.mykindos.betterpvp.game.impl.domination.model.CapturePoint;
import me.mykindos.betterpvp.game.impl.domination.model.attribute.KillScoreAttribute;
import me.mykindos.betterpvp.game.impl.event.PlayerContributePointsEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

@GameScoped
public class DominationListener implements Listener {

    private final Domination game;
    private final GameController gameController;
    private final ServerController serverController;
    private final PlayerController playerController;
    private final GamePlugin plugin;
    private final KillScoreAttribute killScoreAttribute;
    private BukkitTask ticker;

    @Inject
    public DominationListener(Domination game, GameController gameController, ServerController serverController,
                              PlayerController playerController, GamePlugin plugin, KillScoreAttribute killScoreAttribute) {
        this.game = game;
        this.gameController = gameController;
        this.serverController = serverController;
        this.playerController = playerController;
        this.plugin = plugin;
        this.killScoreAttribute = killScoreAttribute;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                gameController.tick();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        serverController.getStateMachine().addExitHandler(GameState.IN_GAME, oldState -> {
            ticker.cancel(); // Cancel the task when game ends
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process during active game
        if (serverController.getCurrentState() != GameState.IN_GAME) {
            return;
        }

        final Player player = event.getPlayer();
        if (!playerController.getParticipant(player).isAlive() || !event.hasChangedPosition()) {
            return; // Only process if player is alive and has moved
        }

        final Team team = game.getPlayerTeam(player);
        if (team == null) {
            return; // Only process if player is on a team
        }

        // Check if player is on a capture point
        for (CapturePoint point : gameController.getCapturePoints()) {
            if (point.isInRange(player.getLocation())) {
                point.addPlayer(player, team);
                break;
            }
        }
    }
    
    @EventHandler
    public void onPlayerDeath(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player) || !(event.getKiller() instanceof Player killer)) {
            return;
        }
        
        // Award points for kill
        final Team team = Objects.requireNonNull(game.getPlayerTeam(killer));
        gameController.addPoints(team, killScoreAttribute.getValue());
        new PlayerContributePointsEvent(killer, killScoreAttribute.getValue()).callEvent();
    }
}