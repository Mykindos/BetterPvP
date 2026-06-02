package me.mykindos.betterpvp.core.scene.mob.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.mob.Disposition;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.core.utilities.events.GetEntityRelationshipEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges {@link SceneMob} relationships into the shared relationship events so that existing
 * skills, AoE, and targeting treat custom mobs correctly with no changes elsewhere.
 * <p>
 * This is the "hybrid" faction model in action:
 * <ul>
 *   <li><b>Owned FRIENDLY mobs</b> inherit their owner's relationships - we delegate to
 *       {@link UtilEntity#getRelation(LivingEntity, LivingEntity)} for the owner, which the clans
 *       module answers. The owner is always a friend.</li>
 *   <li><b>Mob-vs-mob</b> uses the {@link FactionService} relation matrix.</li>
 *   <li>Otherwise the mob's {@link Disposition} decides (HOSTILE = enemy, others = friendly).</li>
 * </ul>
 * Runs at {@link EventPriority#HIGH} so it overrides the default clan classification for any
 * pairing that involves one of our mobs (the clans listener only classifies player-vs-player).
 */
@BPvPListener
@Singleton
public class FactionRelationListener implements Listener {

    private final SceneObjectRegistry registry;
    private final FactionService factionService;

    @Inject
    private FactionRelationListener(SceneObjectRegistry registry, FactionService factionService) {
        this.registry = registry;
        this.factionService = factionService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRelationship(GetEntityRelationshipEvent event) {
        final EntityProperty property = resolve(event.getEntity(), event.getTarget());
        if (property != null) {
            event.setEntityProperty(property);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFetchNearby(FetchNearbyEntityEvent<?> event) {
        final LivingEntity source = event.getSource();
        for (KeyValue<? extends LivingEntity, EntityProperty> entry : event.getEntities()) {
            final EntityProperty property = resolve(source, entry.getKey());
            if (property != null) {
                entry.setValue(property);
            }
        }
    }

    /**
     * Resolves {@code viewer}'s stance toward {@code other} when either is a {@link SceneMob}.
     * Returns {@code null} when neither is a custom mob, leaving the relationship to other
     * listeners (e.g. the clan player-vs-player classification).
     */
    @Nullable
    private EntityProperty resolve(LivingEntity viewer, LivingEntity other) {
        final SceneMob viewerMob = registry.getObject(viewer, SceneMob.class);
        if (viewerMob != null) {
            return stance(viewerMob, other);
        }
        final SceneMob otherMob = registry.getObject(other, SceneMob.class);
        if (otherMob != null) {
            // Enmity/friendship is symmetric enough for filtering: a player's view of a mob is the
            // mob's view of the player.
            return stance(otherMob, viewer);
        }
        return null;
    }

    private EntityProperty stance(SceneMob mob, LivingEntity other) {
        // The owner is always a friend.
        if (mob.getOwner() != null && mob.getOwner().equals(other.getUniqueId())) {
            return EntityProperty.FRIENDLY;
        }
        // Owned FRIENDLY mobs inherit their owner's relationships (delegates to the clans listener).
        if (mob.getDisposition() == Disposition.FRIENDLY && mob.getOwner() != null) {
            final Player ownerPlayer = Bukkit.getPlayer(mob.getOwner());
            if (ownerPlayer != null) {
                return UtilEntity.getRelation(ownerPlayer, other);
            }
        }
        // Mob-vs-mob faction relations.
        final SceneMob otherMob = registry.getObject(other, SceneMob.class);
        if (otherMob != null && mob.getFaction() != null && otherMob.getFaction() != null) {
            return fromFaction(mob, factionService.getRelation(mob.getFaction(), otherMob.getFaction()));
        }
        return fromDisposition(mob);
    }

    private EntityProperty fromFaction(SceneMob mob, FactionRelation relation) {
        return switch (relation) {
            case ALLY -> EntityProperty.FRIENDLY;
            case ENEMY -> EntityProperty.ENEMY;
            // A neutral faction is only an enemy to an actively hostile mob; otherwise left friendly
            // (neutral mobs engage via threat/retaliation, not via this relationship).
            case NEUTRAL -> mob.getDisposition() == Disposition.HOSTILE ? EntityProperty.ENEMY : EntityProperty.FRIENDLY;
        };
    }

    private EntityProperty fromDisposition(SceneMob mob) {
        return mob.getDisposition() == Disposition.HOSTILE ? EntityProperty.ENEMY : EntityProperty.FRIENDLY;
    }

}
