package me.mykindos.betterpvp.game.framework.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.PlayerStatsForGame;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages player name colors in the tab list
 */
@BPvPListener
@Singleton
@CustomLog
public class PlayerListManager implements Listener {

    private final ServerController serverController;
    private final Scoreboard scoreboard;

    @Getter
    private final Map<Player, PlayerStatsForGame> playerStats = new WeakHashMap<>();

    private static final String KILL_ICON_CHAR = "\uD83D\uDDE1";
    private static final String DEATH_ICON_CHAR = "☠";
    private static final String POINTS_ICON_CHAR = "✦";

    @Inject
    public PlayerListManager(ServerController serverController) {
        this.serverController = serverController;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Setup state handlers
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        // Update tab colors when game state changes
        serverController.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> 
            updateAllPlayerTabColors());
            
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> 
            updateAllPlayerTabColors());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Set player color when they join
        updatePlayerTabColor(event.getPlayer());
    }

    /**
     * Gets the tab color for a player based on team/game state.
     */
    private TextColor getTabColorForPlayer(final @NotNull Player player) {
        TextColor color = NamedTextColor.WHITE;
        if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
            final Team playerTeam = teamGame.getPlayerTeam(player);
            if (playerTeam != null) {
                color = playerTeam.getProperties().color();
            }
        }
        return color;
    }

    /**
     * Updates the tab color for a player based on their team
     * 
     * @param player The player to update
     */
    public void updatePlayerTabColor(final @NotNull Player player) {
        // Forward to unified update so color and playerlist entry stay in sync
        updatePlayerList(player);
    }
    
    /**
     * Sets a player's tab list color (scoreboard team)
     * 
     * @param player The player
     * @param color The color to set
     */
    private void setPlayerTabColor(final @NotNull Player player, final @NotNull TextColor color) {
        final String teamName = getTeamNameForColor(color);
        
        // Remove player from all existing teams first
        removePlayerFromTeams(player);
        
        // Get or create scoreboard team
        org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.color(NamedTextColor.nearestTo(color));
        }
        
        // Add player to team
        team.addEntry(player.getName());
    }
    
    /**
     * Removes a player from all scoreboard teams
     * 
     * @param player The player to remove
     */
    private void removePlayerFromTeams(Player player) {
        for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }
    
    /**
     * Gets a team name for a color
     * 
     * @param color The color
     * @return The team name
     */
    private String getTeamNameForColor(TextColor color) {
        return "color_custom_" + color.asHexString().substring(1);
    }
    
    /**
     * Updates tab list colors for all online players
     */
    public void updateAllPlayerTabColors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerList(player);
        }
    }

    /**
     * Adds a kill to the player's stats and updates their tab list entry.
     */
    public void addKill(@NotNull Player player) {
        final @NotNull PlayerStatsForGame stats = playerStats.get(player);
        stats.setKills(stats.getKills() + 1);
        updatePlayerList(player);
    }

    /**
     * Adds a death to the player's stats and updates their tab list entry.
     */
    public void addDeath(@NotNull Player player) {
        final @NotNull PlayerStatsForGame stats = playerStats.get(player);
        stats.setDeaths(stats.getDeaths() + 1);
        updatePlayerList(player);
    }

    /**
     * Adds the specified number of points to the player's states and updates their tab list entry.
     */
    public void addPoints(@NotNull Player player, int points) {
        final @NotNull PlayerStatsForGame stats = playerStats.get(player);
        stats.setPoints(stats.getPoints() + points);
        updatePlayerList(player);
    }

    /**
     * Updates a player's tab list entry and ensures their team color is kept in sync.
     */
    public void updatePlayerList(final @NotNull Player player) {

        final @NotNull String baseName = player.getName();
        final TextColor color = getTabColorForPlayer(player);
        setPlayerTabColor(player, color);
        final Component nameComponent = Component.text(baseName, NamedTextColor.nearestTo(color));

        if (serverController.getCurrentState().equals(GameState.WAITING)
                || serverController.getCurrentState().equals(GameState.STARTING)) {
            player.playerListName(nameComponent);
            return;
        }
        if (!playerStats.containsKey(player)) {
            playerStats.put(player, new PlayerStatsForGame());
        }

        final @NotNull PlayerStatsForGame stats = playerStats.get(player);

        // compute right-aligned kill icon column for names up to 16 chars (min 1 space for longer names handled elsewhere)
        final int spaceCount = Math.max(1, 16 - baseName.length());
        final @NotNull String padding = (" ").repeat(spaceCount);

        final @NotNull Component killComponent = Component.text(padding + KILL_ICON_CHAR + " " + stats.getKills(), NamedTextColor.GREEN);
        final @NotNull Component deathComponent = Component.text(" " + DEATH_ICON_CHAR + " " + stats.getDeaths(), NamedTextColor.RED);
        final @NotNull Component pointsComponent = Component.text(" " + POINTS_ICON_CHAR + " " + stats.getPoints(), NamedTextColor.GOLD);

        player.playerListName(nameComponent.append(killComponent).append(deathComponent).append(pointsComponent));
    }
}