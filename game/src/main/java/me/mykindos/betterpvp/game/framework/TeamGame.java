package me.mykindos.betterpvp.game.framework;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.game.framework.configuration.TeamGameConfiguration;
import me.mykindos.betterpvp.game.framework.manager.PlayerListManager;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Represents a team game with an assigned {@link TeamGameConfiguration}
 */
@CustomLog
public abstract non-sealed class TeamGame extends AbstractGame<TeamGameConfiguration> {

    @Getter
    private final Map<TeamProperties, Team> teams = new HashMap<>();
    private final PlayerListManager playerListManager;

    protected TeamGame(@NotNull TeamGameConfiguration configuration) {
        super(configuration);
        this.playerListManager = injector.getInstance(PlayerListManager.class);
        initializeTeams();
    }

    /**
     * Initialize teams from configuration
     */
    private void initializeTeams() {
        for (TeamProperties properties : getConfiguration().getTeamProperties()) {
            teams.put(properties, new Team(properties, new HashSet<>()));
            log.info("Initialized team: {}", properties.name()).submit();
        }
    }


    /**
     * Adds a player to a team
     *
     * @param player The player to add
     * @param team The team to add the player to
     * @return True if successful, false if the team is full or doesn't exist
     */
    public boolean addPlayerToTeam(Player player, Team team) {
        if (team == null || !teams.containsKey(team.getProperties())) {
            return false;
        }

        // Check if team is full
        if (team.getPlayers().size() >= team.getProperties().size()) {
            return false;
        }

        // Remove from current team first
        removePlayerFromTeam(player);

        // Add to new team
        team.getPlayers().add(player);

        // Update player tab color
        playerListManager.updatePlayerTabColor(player);
        return true;
    }

    /**
     * Adds a player to a team by properties
     *
     * @param player The player to add
     * @param teamProperties The team properties
     * @return True if successful, false if the team is full or doesn't exist
     */
    public boolean addPlayerToTeam(Player player, TeamProperties teamProperties) {
        Team team = teams.get(teamProperties);
        return addPlayerToTeam(player, team);
    }

    /**
     * Removes a player from their current team
     *
     * @param player The player to remove
     * @return The team the player was removed from, or null if not in a team
     */
    @Nullable
    public Team removePlayerFromTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.getPlayers().remove(player)) {
                // Update player tab color
                playerListManager.updatePlayerTabColor(player);
                return team;
            }
        }
        return null;
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

    /**
     * Clears all teams, removing all players
     */
    public void clearTeams() {
        for (Team team : teams.values()) {
            team.getPlayers().clear();
        }
    }
}