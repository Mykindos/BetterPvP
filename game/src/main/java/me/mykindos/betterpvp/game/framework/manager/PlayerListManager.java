package me.mykindos.betterpvp.game.framework.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Manages player name colors in the tab list
 */
@BPvPListener
@Singleton
@CustomLog
public class PlayerListManager implements Listener {

    private final ServerController serverController;
    private final Scoreboard scoreboard;

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
     * Updates the tab color for a player based on their team
     * 
     * @param player The player to update
     */
    public void updatePlayerTabColor(Player player) {
        // Default color is yellow
        TextColor color = NamedTextColor.YELLOW;
        
        // If in a team game, use team color
        if (serverController.getCurrentGame() instanceof TeamGame teamGame) {
            Team playerTeam = teamGame.getPlayerTeam(player);
            if (playerTeam != null) {
                color = playerTeam.getProperties().color();
            }
        }
        
        setPlayerTabColor(player, color);
    }
    
    /**
     * Sets a player's tab list color
     * 
     * @param player The player
     * @param color The color to set
     */
    private void setPlayerTabColor(Player player, TextColor color) {
        String teamName = getTeamNameForColor(color);
        
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
            updatePlayerTabColor(player);
        }
    }
}