package me.mykindos.betterpvp.game.framework.listener.team;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Comparator;
import java.util.Objects;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.event.GameChangeEvent;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.AllowLateJoinsAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.team.DurationBeforeAutoBalanceAttribute;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantDeathEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

@BPvPListener
@Singleton
public class TeamBalancerHandler implements Listener {

    private final GamePlugin plugin;
    private final ServerController serverController;
    private final PlayerController playerController;
    @Nullable
    private BukkitTask balanceTask = null;

    @Inject
    public TeamBalancerHandler(GamePlugin plugin, ServerController serverController, PlayerController playerController) {
        this.plugin = plugin;
        this.serverController = serverController;
        this.playerController = playerController;
        setupStateHandlers();
    }

    private void startBalanceTask(TeamGame<?> game) {
        final DurationBeforeAutoBalanceAttribute durationBeforeAutoBalanceAttribute = game.getConfiguration().getDurationBeforeAutoBalanceAttribute();
        /**
         * Runs this operation.
         */
        this.balanceTask = new BukkitRunnable()  {
            long ticks = 0;

            /**
             * Runs this operation.
             */
            @Override
            public void run() {
                final long totalTicks = durationBeforeAutoBalanceAttribute.getValue().toSeconds() * 20;

                ticks++;
                if (ticks >= totalTicks && !game.isBalanced()) {
                    game.balanceTeams();
                    cancel();
                }
            }
        }.runTaskTimer(
                plugin,
                1,
                1
        );
    }

    private void endBalanceTask() {
        if (balanceTask != null) {
            balanceTask.cancel();
        }
        balanceTask = null;
    }

    private void setupStateHandlers() {
        // Balance teams when transitioning to IN_GAME state
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
                teamGame.balanceTeams();
            }
        });

        // Reset every team when the game ends
        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> {
            if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
                teamGame.resetTeams();
                endBalanceTask();
            }
        });
    }

    @EventHandler
    public void onGameChange(GameChangeEvent event) {
        if (!(event.getNewGame() instanceof final TeamGame<?> teamGame)) return;
        teamGame.getAttribute(AllowLateJoinsAttribute.class)
                .addChangeListener((previous, next) -> {
                    if (Boolean.TRUE.equals(next) && Boolean.FALSE.equals(previous)) {
                        if (!teamGame.isBalanced()) {
                            teamGame.balanceTeams();
                            endBalanceTask();
                        }
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStartSpectate(final ParticipantStartSpectatingEvent event) {
        //only do players that are only spectating this game
        if (event.getParticipant().isSpectateNextGame()) return;
        final AbstractGame<?, ?> game = serverController.getCurrentGame();
        if (!(game instanceof final TeamGame<?> teamGame)) return;
        final boolean isAllowLateJoins = teamGame.getConfiguration().getAllowLateJoinsAttribute().getValue();
        //if this game does not allow late joins, don't try to balance a spectator
        if (!isAllowLateJoins) return;
        //if the game is currently balanced, do nothing
        if (teamGame.isBalanced()) return;

        //balance the teams after this event
        UtilServer.runTaskLater(plugin, () -> {
            //put the player on the lowest team

            final Team lowestTeam = teamGame.getParticipants().stream()
                    .min(Comparator.comparingDouble(team -> (double) team.getParticipants().size() / team.getProperties().size()))
                    .orElseThrow();

            if (teamGame.addPlayerToTeam(event.getParticipant(), lowestTeam)) {
                playerController.setSpectating(event.getPlayer(), event.getParticipant(), false, false);
            }

            //if teams are now balanced, end the balance task
            if (teamGame.isBalanced()) {
                endBalanceTask();
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onParticipantDeath(final ParticipantDeathEvent event) {
        final AbstractGame<?, ?> game = serverController.getCurrentGame();
        if (!(game instanceof final TeamGame<?> teamGame)) return;

        //if this game does not do force balancing, return
        final boolean forceBalance = teamGame.getConfiguration().getForceBalanceAttribute().getValue();
        if (!forceBalance) return;

        final boolean autoBalanceOnDeath = teamGame.getConfiguration().getAutoBalanceOnDeathAttribute().getValue();

        //if this game does not auto balance on death, return
        if (!autoBalanceOnDeath) return;

        //if this game is currently balanced, do nothing
        if (teamGame.isBalanced()) return;

        final double highestPercentage = teamGame.getParticipants().stream()
                .map(team -> (double) team.getParticipants().size() / team.getProperties().size())
                .max(Double::compare)
                .orElseThrow();

        final Team playerTeam = Objects.requireNonNull(teamGame.getPlayerTeam(event.getPlayer()), "Player must be on a team to be dead");
        final double teamPercentage = (double) playerTeam.getParticipants().size() / playerTeam.getProperties().size();
        //if the dead player is not on the highest team, return
        if (highestPercentage != teamPercentage) return;

        UtilServer.runTaskLater(plugin, () -> {
            //move them to the lowest team
            final Team lowestTeam = teamGame.getParticipants().stream()
                    .min(Comparator.comparingDouble(team -> (double) team.getParticipants().size() / team.getProperties().size()))
                    .orElseThrow();

            teamGame.removePlayerFromTeam(event.getParticipant());
            teamGame.addPlayerToTeam(event.getParticipant(), lowestTeam);
            UtilMessage.message(event.getPlayer(), "Team", Component.text("You were moved to ", NamedTextColor.GRAY)
                    .append(Component.text(lowestTeam.getProperties().name(), lowestTeam.getProperties().color(), TextDecoration.BOLD))
                    .append(Component.text(" team for balance.", NamedTextColor.GRAY)));

            //if the game is now balanced, end the balance task
            if (teamGame.isBalanced()) {
                endBalanceTask();
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final AbstractGame<?, ?> game = serverController.getCurrentGame();
        if (!(game instanceof final TeamGame<?> teamGame)) return;
        //if teams are still or now balanced, cancel the balance task
        if (teamGame.isBalanced()) {
            endBalanceTask();
            return;
        }

        final boolean allowLateJoins = teamGame.getConfiguration().getAllowLateJoinsAttribute().getValue();

        //there are players spectating, trigger balance early
        if (allowLateJoins && !playerController.getThisGameSpectators().isEmpty()) {
            teamGame.balanceTeams();
            endBalanceTask();
            return;
        }

        //if the timer is already ticking, keep it running
        if (balanceTask != null && !balanceTask.isCancelled()) return;
        //start the balance timer

        startBalanceTask(teamGame);
    }
}
