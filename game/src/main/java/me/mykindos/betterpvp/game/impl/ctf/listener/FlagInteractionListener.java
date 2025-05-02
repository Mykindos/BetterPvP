package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlag;
import me.mykindos.betterpvp.game.impl.ctf.controller.FlagInventoryCache;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import me.mykindos.betterpvp.game.impl.ctf.model.Flag;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

/**
 * Event listener for flag interactions (pickup and capture)
 */
@CustomLog
@GameScoped
public class FlagInteractionListener implements Listener {

    private final GamePlugin plugin;
    private final CaptureTheFlag game;
    private final GameController gameController;
    private final PlayerController playerController;
    private final FlagInventoryCache flagHotBarCache;

    @Inject
    public FlagInteractionListener(GamePlugin gamePlugin, CaptureTheFlag game, GameController gameController,
                                   PlayerController playerController, FlagInventoryCache flagHotBarCache) {
        this.plugin = gamePlugin;
        this.game = game;
        this.gameController = gameController;
        this.playerController = playerController;
        this.flagHotBarCache = flagHotBarCache;
    }

    @EventHandler
    public void onCapture(PlayerMoveEvent event) {
        if (!playerController.getParticipant(event.getPlayer()).isAlive() || !event.hasExplicitlyChangedPosition()) {
            return; // Only check when player moves to a new block and is a participant
        }

        final Player player = event.getPlayer();
        if (!flagHotBarCache.hasCache(player)) {
            return; // A bit of a hack, but prevents player from capturing flag if they don't have it
        }

        final Team team = game.getPlayerTeam(player);
        if (team == null) {
            return; // No team
        }

        final Flag selfFlag = gameController.getFlag(team);
        if (selfFlag.getBaseLocation().distanceSquared(player.getLocation()) > 2.0 * 2.0) {
            return; // Player must be at their base to capture
        }

        // Get flag the player is holding
        final Optional<Flag> flagOpt = gameController.getFlags().values().stream()
                .filter(flag -> flag.getHolder() == player)
                .findAny();
        if (flagOpt.isEmpty()) {
            return;
        }

        Flag flag = flagOpt.get();
        gameController.scoreCapture(team, flag);
        flag.capture();
    }
    
    @EventHandler
    public void onPickup(PlayerMoveEvent event) {
        if (!event.hasExplicitlyChangedPosition()) {
            return; // Only check when player moves to a new block
        }

        final Player player = event.getPlayer();
        if (flagHotBarCache.hasCache(player)) {
            return; // A bit of a hack, but prevents player from picking up flag while another is picked up
        }

        if(player.getGameMode() == GameMode.SPECTATOR) return;

        final Team team = game.getPlayerTeam(player);
        if (team == null) {
            return; // No team
        }

        // Get flag at player location
        final TeamProperties teamProperties = team.getProperties();
        final Optional<Flag> flagOpt = gameController.getFlags().values().stream()
                .filter(flag -> !teamProperties.equals(flag.getTeam().getProperties())) // Must be enemy flag
                .filter(Flag::canPickup) // Only pickup if not on cooldown
                .filter(entry -> isOnFlag(entry, player.getLocation()))
                .findAny();
        if (flagOpt.isEmpty()) {
            return; // No flag
        }

        final Flag flag = flagOpt.get();
        flag.pickup(team, player);
    }

    private boolean isOnFlag(Flag flag, Location location) {
        final Location leveled = location.clone();
        if (flag.getCurrentLocation().getWorld() != location.getWorld() || flag.getCurrentLocation().distanceSquared(leveled) > 1.5) {
            return false;
        }

        final double heightDiff = location.getY() - flag.getCurrentLocation().getY();
        return heightDiff > -1 && heightDiff < flag.getSize() + 1;
    }
    
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        // Check if player pressed Q and is holding flag
        Player player = event.getPlayer();
        
        for (Flag flag : gameController.getFlags().values()) {
            if (player.equals(flag.getHolder())) {
                // Drop the flag at player location
                // in a task because it causes a dupe for some reason, setting the inventory right after the event
                UtilServer.runTaskLater(plugin, () -> {
                    flag.drop(player.getLocation());
                }, 1L);
                break;
            }
        }
    }
}