package me.mykindos.betterpvp.core.scene.mob.target;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Composable factories producing {@link TargetSelector} strategies for {@link SceneMob}s.
 * <p>
 * Each method returns a stateless selector (implemented as a lambda) that the targeting component
 * runs every tick. Selectors can be wrapped to add constraints &mdash; for example
 * {@link #withLineOfSight(TargetSelector)} filters another selector's result down to targets the
 * mob can actually see.
 */
@UtilityClass
public class TargetSelectors {

    /**
     * Selects the nearest enemy within {@code radius} blocks of the mob.
     *
     * @param radius the search radius in blocks
     * @return a selector returning the closest enemy by squared distance, or empty if none are nearby
     */
    public TargetSelector nearestEnemy(double radius) {
        return mob -> {
            final LivingEntity source = (LivingEntity) mob.getEntity();
            return getEnemies(source, radius)
                    .min(Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(source.getLocation())));
        };
    }

    /**
     * Selects the lowest-health enemy within {@code radius} blocks of the mob, useful for finishing
     * off weakened targets.
     *
     * @param radius the search radius in blocks
     * @return a selector returning the enemy with the smallest current health, or empty if none are nearby
     */
    public TargetSelector lowestHealthEnemy(double radius) {
        return mob -> {
            final LivingEntity source = (LivingEntity) mob.getEntity();
            return getEnemies(source, radius)
                    .min(Comparator.comparingDouble(LivingEntity::getHealth));
        };
    }

    /**
     * Selects the entity that has accumulated the most threat against the mob (its current aggro
     * leader), provided that entity is still a valid, living target in the mob's world.
     *
     * @return a selector returning the highest-threat living target, or empty if there is none
     */
    public TargetSelector highestThreat() {
        return mob -> {
            final Optional<UUID> highest = mob.getThreat().highest();
            if (highest.isEmpty()) {
                return Optional.empty();
            }

            final Entity entity = Bukkit.getEntity(highest.get());
            if (!(entity instanceof LivingEntity target) || !mob.isValidTarget(target)) {
                return Optional.empty();
            }
            return Optional.of(target);
        };
    }

    /**
     * Wraps another selector, keeping its chosen target only when the mob has a clear line of sight
     * to it. If the underlying entity is not a {@link Mob} (and so cannot raycast sight) the result
     * is discarded.
     *
     * @param delegate the selector whose result should be sight-filtered
     * @return a selector yielding the delegate's target only when it is visible to the mob
     */
    public TargetSelector withLineOfSight(TargetSelector delegate) {
        return mob -> {
            final Mob bukkitMob = mob.getBukkitMob();
            return delegate.select(mob)
                    .filter(target -> bukkitMob != null && bukkitMob.hasLineOfSight(target));
        };
    }

    private Stream<LivingEntity> getEnemies(LivingEntity livingEntity, double radius) {
        return UtilEntity.getNearbyEntities(livingEntity, radius).stream()
                .map(KeyValue::getKey)
                .filter(entity -> UtilEntity.IS_ENEMY.test(livingEntity, entity));
    }

}
