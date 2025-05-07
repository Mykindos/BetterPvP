package me.mykindos.betterpvp.game.framework.listener.team;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class TeamBalancerListener implements Listener {

    private static final Random RANDOM = new Random();

    private final ServerController serverController;
    private final PlayerController playerController;

    @Inject
    public TeamBalancerListener(ServerController serverController, PlayerController playerController) {
        this.serverController = serverController;
        this.playerController = playerController;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        // Balance teams when transitioning to IN_GAME state
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            if (serverController.getCurrentGame() instanceof TeamGame<?>teamGame) {
                balanceTeams(teamGame);
            }
        });

        // Reset every team when the game ends
        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> {
            if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
                teamGame.resetTeams();
            }
        });
    }

    /**
     * Balances teams by moving players between teams to ensure they are evenly distributed
     * @param teamGame The team game to balance
     */
    private void balanceTeams(TeamGame<?> teamGame) {
        List<Participant> participants = new ArrayList<>(playerController.getParticipants().values());
        Map<Team, Integer> teamSizes = new ConcurrentHashMap<>();
        List<Participant> unassignedPlayers = new ArrayList<>();

        // Get current team assignments and identify unassigned players
        for (Participant participant : participants) {
            Team team = teamGame.getPlayerTeam(participant.getPlayer());
            if (team != null) {
                teamSizes.put(team, teamSizes.getOrDefault(team, 0) + 1);
            } else {
                unassignedPlayers.add(participant);
            }
        }

        Collections.shuffle(unassignedPlayers, RANDOM);

        // Calculate target size for each team
        int totalPlayers = participants.size();
        int numTeams = teamGame.getTeams().size();
        int basePlayersPerTeam = totalPlayers / numTeams;
        int extraPlayers = totalPlayers % numTeams;

        // Calculate target sizes for each team
        Map<Team, Integer> targetSizes = new HashMap<>();
        for (Team team : teamGame.getTeams().values()) {
            int target = basePlayersPerTeam;
            if (extraPlayers > 0) {
                target++;
                extraPlayers--;
            }
            targetSizes.put(team, target);
        }

        // First, assign unassigned players to teams that need more players
        for (Participant participant : unassignedPlayers) {
            Team targetTeam = teamGame.getTeams().values().stream()
                    .filter(team -> teamSizes.getOrDefault(team, 0) < targetSizes.get(team))
                    .min(Comparator.comparingInt(team -> teamSizes.getOrDefault(team, 0)))
                    .orElse(null);

            if (targetTeam != null) {
                teamGame.removePlayerFromTeam(participant);
                teamGame.addPlayerToTeam(participant, targetTeam);
                teamSizes.put(targetTeam, teamSizes.getOrDefault(targetTeam, 0) + 1);
            }
        }

        // Second, if teams are still unbalanced, move players from overpopulated teams
        List<Participant> playersToReassign = new ArrayList<>();
        Map<Team, Integer> excessPlayers = new HashMap<>();

        // Calculate excess players in each team
        for (Team team : teamGame.getTeams().values()) {
            int current = teamSizes.getOrDefault(team, 0);
            int target = targetSizes.get(team);
            int excess = current - target;

            if (excess > 0) {
                excessPlayers.put(team, excess);
            }
        }

        // Find players to move from overpopulated teams
        for (Map.Entry<Team, Integer> entry : excessPlayers.entrySet()) {
            Team team = entry.getKey();
            int excess = entry.getValue();

            // Get players who manually selected this team
            List<Participant> teamPlayers = new ArrayList<>(team.getParticipants());
            for (int i = 0; i < excess && i < teamPlayers.size(); i++) {
                playersToReassign.add(teamPlayers.get(i));
                teamSizes.put(team, teamSizes.get(team) - 1);
            }
        }

        // Reassign excess players to underpopulated teams
        for (Participant participant : playersToReassign) {
            Team targetTeam = teamGame.getTeams().values().stream()
                    .filter(team -> teamSizes.getOrDefault(team, 0) < targetSizes.get(team))
                    .min(Comparator.comparingInt(team -> teamSizes.getOrDefault(team, 0)))
                    .orElse(null);

            if (targetTeam != null) {
                teamGame.removePlayerFromTeam(participant);
                teamGame.addPlayerToTeam(participant, targetTeam);
                teamSizes.put(targetTeam, teamSizes.getOrDefault(targetTeam, 0) + 1);
                UtilMessage.message(participant.getPlayer(), "Team", Component.text("You were moved to ", NamedTextColor.GRAY)
                        .append(Component.text(targetTeam.getProperties().name(), targetTeam.getProperties().color(), TextDecoration.BOLD))
                        .append(Component.text(" team for balance.", NamedTextColor.GRAY)));
            }
        }

        //TODO add option to balance over size players or send to spectator
        //Finally, any remaining players should be put into spectator
        for (Participant participant : participants) {
            if (teamGame.getPlayerTeam(participant.getPlayer()) != null) continue;
            playerController.setSpectating(participant.getPlayer(), participant, true, false);
        }
    }

}
