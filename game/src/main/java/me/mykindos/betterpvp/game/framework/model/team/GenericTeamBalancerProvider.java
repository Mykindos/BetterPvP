package me.mykindos.betterpvp.game.framework.model.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.AllowLateJoinsAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.team.MaxImbalanceAttribute;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;


public class GenericTeamBalancerProvider implements TeamBalancerProvider {

    /**
     * Balances teams by moving players between teams to ensure they are evenly distributed
     * @param teamGame The team game to balance
     */
    public void balanceTeams(TeamGame<?> teamGame) {
        final PlayerController playerController = JavaPlugin.getPlugin(GamePlugin.class).getInjector().getInstance(PlayerController.class);
        boolean allowLateJoins = teamGame.getConfiguration().getAllowLateJoinsAttribute().getValue();
        boolean forceBalance = teamGame.getConfiguration().getForceBalanceAttribute().getValue();


        List<Participant> participants = new ArrayList<>(playerController.getParticipants().values());
        //if we allow late joins, add all this game spectators to unassigned players

        if (allowLateJoins) {
            participants.addAll(playerController.getThisGameSpectators().values());
        }

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

        Collections.shuffle(unassignedPlayers, UtilMath.RANDOM);

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
                if (teamGame.addPlayerToTeam(participant, targetTeam)) {
                    //since unassigned players might be spectating, we need to remove that before assigning teams
                    playerController.setSpectating(participant.getPlayer(), participant, false, false);
                    teamSizes.put(targetTeam, teamSizes.getOrDefault(targetTeam, 0) + 1);
                }

            }
        }

        if (forceBalance) {
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
                        .min(Comparator.comparingDouble(team -> (double) team.getParticipants().size() / team.getProperties().size()))
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
        }

        //Finally, any remaining players should be put into spectator
        for (Participant participant : participants) {
            if (teamGame.getPlayerTeam(participant.getPlayer()) != null) continue;
            playerController.setSpectating(participant.getPlayer(), participant, true, false);
        }
    }

    /**
     * Returns true if the teams are balanced,
     * as in all teams have a difference of less than or equal to {@link MaxImbalanceAttribute#getValue() max imbalance}
     *
     * @param teamGame the {@link TeamGame} to check
     * @return {@code true} if all teams are within {@link MaxImbalanceAttribute#getValue() max imbalance} of each other
     * and there are no waiting spectators when {@link AllowLateJoinsAttribute#getValue() allow late joins} is {@code true},
     * {@code false} otherwise
     */
    @Override
    public boolean isBalanced(TeamGame<?> teamGame) {
        final PlayerController playerController = JavaPlugin.getPlugin(GamePlugin.class).getInjector().getInstance(PlayerController.class);
        boolean allowRespawns = teamGame.getConfiguration().getRespawnsAttribute().getValue();


        //games without respawns are always balanced
        if (!allowRespawns) {
            return true;
        }

        boolean allowJoins = teamGame.getConfiguration().getAllowLateJoinsAttribute().getValue();

        if (allowJoins && !playerController.getThisGameSpectators().isEmpty()) {
            return false;
        }

        int maxImbalance = teamGame.getConfiguration().getMaxImbalanceAttribute().getValue();

        int lowest = teamGame.getParticipants().stream()
                .map(team -> team.getParticipants().size())
                .min(Integer::compareTo).orElse(0);
        int highest = teamGame.getParticipants().stream()
                .map(team -> team.getParticipants().size())
                .max(Integer::compareTo).orElse(0);

        boolean balanced = highest - lowest <= maxImbalance;
        return balanced;
    }
}
