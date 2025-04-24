package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.core.utilities.events.GetEntityRelationshipEvent;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class PlayerDamageListener implements Listener {

    private final PlayerController playerController;
    private final ServerController serverController;

    @Inject
    public PlayerDamageListener(PlayerController playerController, ServerController serverController) {
        this.playerController = playerController;
        this.serverController = serverController;
    }

    @EventHandler
    public void onPlayerDeath(CustomDeathEvent event) {
        if (event.getKilled() instanceof Player && serverController.getCurrentState() == GameState.IN_GAME && serverController.getCurrentGame().attemptGracefulEnding()) {
            serverController.transitionTo(GameState.ENDING);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(PreDamageEvent event) {
        final GameState state = serverController.getCurrentState();
        switch (state) {
            // No damage in wait lobbies
            case WAITING, STARTING -> event.setCancelled(true);

            // Only damage alive players
            case IN_GAME, ENDING -> {
                if (!(event.getDamageEvent().getDamagee() instanceof Player player)) {
                    return;
                }

                final Participant participant = playerController.getParticipant(player);
                if (!participant.isAlive()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Disable projectiles targeting dead players
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTarget(EntityCanHurtEntityEvent event) {
        if (!(event.getDamagee() instanceof Player player)) {
            return;
        }

        final Participant participant = playerController.getParticipant(player);
        if (!participant.isAlive()) {
            event.setResult(Event.Result.DENY);
        }
    }

    // Remove dead players from nearby entity fetches, like AoE skills
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFetchNearby(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(pair -> {
            if (!(pair.get() instanceof Player player)) {
                return false;
            }

            final Participant participant = playerController.getParticipant(player);
            return !participant.isAlive();
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRelation(GetEntityRelationshipEvent event) {
        if (!(event.getTarget() instanceof Player player) || !(event.getEntity() instanceof Player damager)) {
            return;
        }

        if (!(serverController.getCurrentGame() instanceof TeamGame<?> game)) {
            return;
        }

        final Participant target = playerController.getParticipant(player);
        if (!target.isAlive()) {
            event.setEntityProperty(EntityProperty.ALL);
        }
    }
}
