package me.mykindos.betterpvp.game.framework.module.powerup;

import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Listens for player interactions with powerup points
 */
public class PowerupListener implements Listener {

    private final PowerupManager powerupManager;
    private final ServerController serverController;
    private final PlayerController playerController;
    private final GamePlugin plugin;
    private BukkitTask ticker;

    PowerupListener(PowerupManager powerupManager, ServerController serverController, PlayerController playerController, GamePlugin plugin) {
        this.powerupManager = powerupManager;
        this.serverController = serverController;
        this.playerController = playerController;
        this.plugin = plugin;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                powerupManager.getPowerups().forEach(Powerup::tick);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        serverController.getStateMachine().addExitHandler(GameState.IN_GAME, oldState -> {
            ticker.cancel(); // Cancel the task when game ends
        });
    }

    @EventHandler
    public void onItemPickup(PlayerMoveEvent event) {
        // Only process during active game
        if (serverController.getCurrentState() != GameState.IN_GAME && serverController.getCurrentState() != GameState.ENDING) {
            return;
        }

        final Participant participant = playerController.getParticipant(event.getPlayer());
        if (!participant.isAlive() || !event.hasChangedPosition()) {
            return;
        }

        Powerup powerup = powerupManager.getPowerupByNearestLocation(event.getTo(), 0.75);
        if (powerup != null && powerup.isEnabled()) {
            final ParticipantPowerupEvent restockEvent = new ParticipantPowerupEvent(event.getPlayer(), participant, powerup);
            restockEvent.callEvent();
            if (restockEvent.isCancelled()) {
                return;
            }

            powerup.use(event.getPlayer());
        }
    }
}