package me.mykindos.betterpvp.game.framework.listener.team;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.function.Function;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathMessageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.core.utilities.events.GetEntityRelationshipEvent;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class TeamDamageListener implements Listener {

    private final ServerController serverController;

    @Inject
    public TeamDamageListener(ServerController serverController) {
        this.serverController = serverController;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(PreDamageEvent event) {
        final GameState state = serverController.getCurrentState();
        if (state != GameState.IN_GAME && state != GameState.ENDING) {
            return;
        }

        if (!(event.getDamageEvent().getDamagee() instanceof Player player) || !(event.getDamageEvent().getDamager() instanceof Player damager)) {
            return;
        }

        if (!(serverController.getCurrentGame() instanceof TeamGame<?> game)) {
            return;
        }

        if (inSameTeam(game, player, damager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRelation(GetEntityRelationshipEvent event) {
        if (!(event.getTarget() instanceof Player player) || !(event.getEntity() instanceof Player damager)) {
            return;
        }

        if (!(serverController.getCurrentGame() instanceof TeamGame<?> game)) {
            return;
        }

        if (inSameTeam(game, player, damager)) {
            event.setEntityProperty(EntityProperty.FRIENDLY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTarget(EntityCanHurtEntityEvent event) {
        if (!(event.getDamagee() instanceof Player player) || !(event.getDamager() instanceof Player damager)) {
            return;
        }

        if (!(serverController.getCurrentGame() instanceof TeamGame<?> game)) {
            return;
        }

        if (inSameTeam(game, player, damager)) {
            event.setResult(Event.Result.DENY);
        }
    }

    // Remove dead players from nearby entity fetches, like AoE skills
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFetchNearby(FetchNearbyEntityEvent<?> event) {

        if (!(event.getSource() instanceof Player player)) {
            return;
        }

        if (!(serverController.getCurrentGame() instanceof TeamGame<?> game)) {
            return;
        }

        event.getEntities().forEach(pair -> {
            if (!(pair.get() instanceof Player other)) {
                return;
            }

            if (inSameTeam(game, player, other)) {
                pair.setValue(EntityProperty.FRIENDLY);
            } else {
                pair.setValue(EntityProperty.ENEMY);
            }
        });
    }

    @EventHandler
    public void onThrowableHitEntity(ThrowableHitEntityEvent event) {

        if(!(event.getThrowable().getThrower() instanceof Player player)) {
            return;
        }

        if(!(event.getCollision() instanceof Player other)) {
            return;
        }

        if (!(serverController.getCurrentGame() instanceof TeamGame<?> game)) {
            return;
        }


        if (other.getGameMode() == GameMode.CREATIVE || other.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        if (!event.getThrowable().isCanHitFriendlies()) {
            if(inSameTeam(game, player, other)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeathMessage(CustomDeathMessageEvent event) {
        if (!(serverController.getCurrentGame() instanceof TeamGame<?> game)) {
            return;
        }

        final Function<LivingEntity, Component> oldFormat = event.getNameFormat();
        event.setNameFormat(livingEntity -> {
            if (!(livingEntity instanceof Player player)) {
                return oldFormat.apply(livingEntity);
            }

            final Team team = game.getPlayerTeam(player);
            if (team == null) {
                return oldFormat.apply(livingEntity);
            }

            return Component.text(player.getName(), team.getProperties().color());
        });
    }

    private boolean inSameTeam(TeamGame<?> game, Player player, Player other) {
        final Team team = game.getPlayerTeam(player);
        final Team otherTeam = game.getPlayerTeam(other);
        return team != null && team == otherTeam;
    }
}
