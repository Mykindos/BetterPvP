package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantDeathEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantReviveEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@BPvPListener
@Singleton
public class PlayerLocationListener implements Listener {

    private final GamePlugin plugin;
    private final PlayerController playerController;
    private final ServerController serverController;
    private final MapManager mapManager;

    @Inject
    public PlayerLocationListener(GamePlugin plugin, PlayerController playerController, ServerController serverController,
                                  MapManager mapManager) {
        this.plugin = plugin;
        this.playerController = playerController;
        this.serverController = serverController;
        this.mapManager = mapManager;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        teleportPlayersToSpawnPoints(); // Default when server starts

        // Enter: WAITING -> Teleport players to spawnpoints
        serverController.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> {
            if (oldState != GameState.STARTING) { // Just so we can swap between WAITING and STARTING without constantly teleporting people
                teleportPlayersToSpawnPoints();
            }
        });

        // Enter: IN_GAME -> Teleport players to spawnpoints
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> teleportPlayersToSpawnPoints());
    }

    /**
     * Teleports all players to available spawpoints.
     */
    private void teleportPlayersToSpawnPoints() {
        for (Player player : playerController.getEverybody().keySet()) {
            teleportToSpawnPoint(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        teleportToSpawnPoint(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(getCurrentSpawnPoint(event.getPlayer()));
    }

    @EventHandler
    public void onRevive(ParticipantReviveEvent event) {
        teleportToSpawnPoint(event.getPlayer());
    }

    @EventHandler
    public void onDeath(ParticipantDeathEvent event) {
        teleportToSpawnPoint(event.getPlayer());
    }

    @EventHandler
    public void onStartSpectate(ParticipantStartSpectatingEvent event) {
        teleportToSpawnPoint(event.getPlayer());
    }

    @EventHandler
    public void onStopSpectate(ParticipantStopSpectatingEvent event) {
        teleportToSpawnPoint(event.getPlayer());
    }

    // Handle teleporting players back to spawn if they leave the map, or kill them
    @EventHandler
    public void onBoundLeave(PlayerMoveEvent event) {
        if (!event.hasExplicitlyChangedBlock()) {
            return;
        }

        final Player player = event.getPlayer();
        switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> {
                if (event.getTo().getY() > mapManager.getWaitingLobby().getMetadata().getMaxHeight()) {
                    teleportToSpawnPoint(player);
                }
            }
            case IN_GAME, ENDING -> {
                final MappedWorld map = mapManager.getCurrentMap();
                final Participant participant = playerController.getParticipant(player);

                if (event.getTo().getY() > map.getMetadata().getMaxHeight()) {
                    if (participant.isAlive()) {
                        final CustomDamageEvent damage = new CustomDamageEvent(player,
                                null,
                                null,
                                EntityDamageEvent.DamageCause.VOID,
                                Integer.MAX_VALUE,
                                false,
                                "Border");
                        UtilDamage.doCustomDamage(damage);
                    } else {
                        teleportToSpawnPoint(player);
                    }
                }
            }
        }
    }

    private void teleportToSpawnPoint(Player player) {
        UtilServer.runTaskLater(plugin, () -> {
            player.teleportAsync(getCurrentSpawnPoint(player));
        }, 1L);
    }

    private Location getCurrentSpawnPoint(Player player) {
        return switch (serverController.getCurrentState()) {
            case WAITING, STARTING -> mapManager.getWaitingLobby().getSpawnpoint().getLocation();
            case IN_GAME, ENDING -> {
                final Participant participant = playerController.getParticipant(player);
                AbstractGame<?, ?> game = serverController.getCurrentGame();
                MappedWorld map = mapManager.getCurrentMap();

                if (!participant.isAlive()) {
                    yield map.getSpawnpoint().getLocation();
                } else {
                    yield game.getConfiguration().getSpawnPointProvider().getSpawnPoint(player, map, game);
                }
            }
        };
    }
}
