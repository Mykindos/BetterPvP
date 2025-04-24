package me.mykindos.betterpvp.game.framework;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.configuration.TeamGameConfiguration;
import me.mykindos.betterpvp.game.framework.manager.PlayerListManager;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a team game with an assigned {@link TeamGameConfiguration}
 */
@CustomLog
public abstract non-sealed class TeamGame<C extends TeamGameConfiguration> extends AbstractGame<C, Team> {

    private final Map<TeamProperties, Team> teams = new HashMap<>();

    protected TeamGame(@NotNull C configuration) {
        super(configuration);
        initializeTeams();
    }

    private void initializeTeams() {
        for (TeamProperties properties : getConfiguration().getTeamProperties()) {
            teams.put(properties, new Team(properties, new HashSet<>()));
            log.info("Initialized team: {}", properties.name()).submit();
        }
    }

    public Map<TeamProperties, Team> getTeams() {
        return Map.copyOf(teams);
    }

    @Override
    public void tearDown() {
        teams.values().forEach(team -> team.getParticipants().clear());
    }

    /**
     * Adds a player to a team
     *
     * @param participant The participant to add the player to
     * @param team The team to add the player to
     * @return True if successful, false if the team is full or doesn't exist
     */
    public boolean addPlayerToTeam(Participant participant, Team team) {
        if (team == null || !teams.containsKey(team.getProperties())) {
            return false;
        }

        // Check if team is full
        if (team.getPlayers().size() >= team.getProperties().size()) {
            return false;
        }

        // Remove from current team first
        removePlayerFromTeam(participant);

        // Add to new team
        team.getParticipants().add(participant);

        // Update player tab color
        GamePlugin.getPlugin(GamePlugin.class).getInjector().getInstance(PlayerListManager.class).updatePlayerTabColor(participant.getPlayer());
        return true;
    }

    /**
     * Removes a player from their current team
     *
     * @param participant The player to remove
     * @return The team the player was removed from, or null if not in a team
     */
    @Nullable
    public Team removePlayerFromTeam(Participant participant) {
        for (Team team : teams.values()) {
            if (team.getParticipants().remove(participant)) {
                // Update player tab color
                GamePlugin.getPlugin(GamePlugin.class).getInjector().getInstance(PlayerListManager.class).updatePlayerTabColor(participant.getPlayer());
                return team;
            }
        }
        return null;
    }

    /**
     * Resets all teams, removing all players
     */
    public void resetTeams() {
        teams.values().forEach(team -> team.getParticipants().clear());

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            JavaPlugin.getPlugin(GamePlugin.class).getInjector().getInstance(PlayerListManager.class).updatePlayerTabColor(onlinePlayer);
        }
    }

    /**
     * Gets the team a player is in
     *
     * @param player The player
     * @return The team, or null if not in a team
     */
    @Nullable
    public Team getPlayerTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.getPlayers().contains(player)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Gets a team by their properties
     *
     * @param properties The team properties
     * @return The team, or null if not found
     */
    @Nullable
    public Team getTeam(TeamProperties properties) {
        return teams.get(properties);
    }

    @Override
    public Set<Team> getParticipants() {
        return Set.copyOf(teams.values());
    }

    @Override
    public boolean attemptGracefulEnding() {
        List<Team> teams = getTeams().values().stream()
                .filter(team -> team.getPlayers().stream().anyMatch(OfflinePlayer::isOnline))
                .toList();
        if (teams.size() == 1) {
            setWinners(teams);
            return true;
        }

        return false;
    }

    @Override
    public void forceEnd() {
        final List<Team> teams = getTeams().values().stream()
                .filter(team -> team.getPlayers().stream().anyMatch(OfflinePlayer::isOnline))
                .toList();
        setWinners(teams);
    }

    @Override
    public Component getWinnerDescription() {
        Preconditions.checkArgument(getWinners().size() == 1, "Only one winner is supported");
        final Team winner = getWinners().getFirst();
        return Component.text(winner.getProperties().name() + " won the game!", winner.getProperties().color(), TextDecoration.BOLD);
    }

}