package me.mykindos.betterpvp.core.utilities.model.selector.shape;

import lombok.Builder;
import lombok.Value;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntityFilter;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntityFilters;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntitySelector;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An entity selector that uses Minecraft's ray tracing to select entities along a line.
 * Supports both single-target (first hit) and piercing (all hits) modes.
 * <p>
 * The ray starts at the origin's position and extends in the origin's facing direction.
 * <p>
 * Usage examples:
 * <pre>
 * // Select first enemy hit within 10 blocks
 * RayTraceEntitySelector.single(new EntityOrigin(player, true), 10.0)
 *     .withFilter(EntityFilters.combatEnemies())
 *     .select();
 *
 * // Select all enemies along a 15-block piercing ray with 0.5 block width
 * RayTraceEntitySelector.piercing(new EntityOrigin(player, true), 15.0, 0.5)
 *     .withFilter(EntityFilters.combatEnemies())
 *     .select();
 *
 * // Ray trace with block collision checking
 * RayTraceEntitySelector.builder()
 *     .origin(new EntityOrigin(player, true))
 *     .maxDistance(20.0)
 *     .raySize(0.25)
 *     .piercing(false)
 *     .ignoreBlocks(false)
 *     .build()
 *     .select();
 * </pre>
 */
@Value
@Builder(toBuilder = true)
public class RayTraceEntitySelector implements EntitySelector<LivingEntity> {

    /**
     * The origin point and direction for the ray.
     */
    SelectorOrigin origin;

    /**
     * The maximum distance the ray will travel.
     */
    double maxDistance;

    /**
     * The size/thickness of the ray for entity collision detection.
     * A value of 0 means a thin ray, larger values create a "thicker" ray.
     */
    @Builder.Default
    double raySize = 0.0;

    /**
     * Whether the ray should pierce through entities and return all hits,
     * or stop at the first entity hit.
     */
    @Builder.Default
    boolean piercing = false;

    /**
     * Whether the ray should ignore blocks.
     * If false, the ray will stop when it hits a block.
     */
    @Builder.Default
    boolean ignoreBlocks = true;

    /**
     * The fluid collision mode for block ray tracing.
     * Only used when ignoreBlocks is false.
     */
    @Builder.Default
    FluidCollisionMode fluidCollisionMode = FluidCollisionMode.NEVER;

    /**
     * The filter to apply to potential entities.
     * Defaults to the standard combat filter.
     */
    @Builder.Default
    EntityFilter filter = EntityFilters.combat();

    /**
     * Optional custom direction for the ray.
     * If null, uses the origin's orientation.
     */
    @Nullable
    Vector direction;

    @Override
    public Collection<LivingEntity> select() {
        Location startLocation = origin.toLocation();
        World world = startLocation.getWorld();
        if (world == null) {
            return Collections.emptyList();
        }

        // Determine ray direction
        Vector rayDirection = direction != null ? direction.clone().normalize() : origin.getOrientation();
        if (rayDirection == null || rayDirection.lengthSquared() < 1e-6) {
            return Collections.emptyList();
        }
        rayDirection = rayDirection.normalize();

        // Create predicate that combines our filter
        Predicate<Entity> entityPredicate = entity -> {
            if (!(entity instanceof LivingEntity living)) {
                return false;
            }
            return filter.test(origin, living);
        };

        // Check for block collision first if not ignoring blocks
        double effectiveMaxDistance = maxDistance;
        if (!ignoreBlocks) {
            RayTraceResult blockHit = world.rayTraceBlocks(startLocation, rayDirection, maxDistance, fluidCollisionMode, true);
            if (blockHit != null) {
                effectiveMaxDistance = blockHit.getHitPosition().distance(startLocation.toVector());
            }
        }

        if (piercing) {
            return selectPiercing(world, startLocation, rayDirection, effectiveMaxDistance, entityPredicate);
        } else {
            return selectSingle(world, startLocation, rayDirection, effectiveMaxDistance, entityPredicate);
        }
    }

    /**
     * Selects the first entity hit by the ray.
     */
    private Collection<LivingEntity> selectSingle(World world, Location start, Vector direction, double maxDist, Predicate<Entity> predicate) {
        RayTraceResult result = world.rayTraceEntities(start, direction, maxDist, raySize, predicate);
        if (result != null && result.getHitEntity() instanceof LivingEntity living) {
            return Collections.singletonList(living);
        }
        return Collections.emptyList();
    }

    /**
     * Selects all entities along the ray path (piercing mode).
     */
    private Collection<LivingEntity> selectPiercing(World world, Location start, Vector direction, double maxDist, Predicate<Entity> basePredicate) {
        List<LivingEntity> hits = new ArrayList<>();
        Set<Entity> alreadyHit = new HashSet<>();

        // Keep tracing until we've covered the full distance
        Location currentStart = start.clone();
        while (true) {
            // Create predicate that excludes already-hit entities
            Predicate<Entity> predicate = entity -> !alreadyHit.contains(entity) && basePredicate.test(entity);

            RayTraceResult result = world.rayTraceEntities(currentStart, direction, maxDist, raySize, predicate);

            if (result == null || result.getHitEntity() == null) {
                break; // No more hits
            }

            Entity hitEntity = result.getHitEntity();
            alreadyHit.add(hitEntity);

            if (hitEntity instanceof LivingEntity living) {
                hits.add(living);
            }
        }

        return hits;
    }

    @Override
    public SelectorOrigin getOrigin() {
        return origin;
    }

    @Override
    public EntitySelector<LivingEntity> withFilter(EntityFilter additionalFilter) {
        return toBuilder()
                .filter(filter.and(additionalFilter))
                .build();
    }

    @Override
    public EntitySelector<LivingEntity> withOrigin(SelectorOrigin newOrigin) {
        return toBuilder()
                .origin(newOrigin)
                .build();
    }

    /**
     * Creates a new selector with a custom ray direction.
     *
     * @param direction the direction vector for the ray
     * @return a new selector with the specified direction
     */
    public RayTraceEntitySelector withDirection(Vector direction) {
        return toBuilder()
                .direction(direction)
                .build();
    }

    /**
     * Creates a single-target ray trace selector (stops at first hit).
     *
     * @param origin      the origin and direction of the ray
     * @param maxDistance the maximum ray distance
     * @return a new single-target RayTraceEntitySelector
     */
    public static RayTraceEntitySelector single(SelectorOrigin origin, double maxDistance) {
        return builder()
                .origin(origin)
                .maxDistance(maxDistance)
                .piercing(false)
                .build();
    }

    /**
     * Creates a single-target ray trace selector with ray thickness.
     *
     * @param origin      the origin and direction of the ray
     * @param maxDistance the maximum ray distance
     * @param raySize     the thickness of the ray
     * @return a new single-target RayTraceEntitySelector
     */
    public static RayTraceEntitySelector single(SelectorOrigin origin, double maxDistance, double raySize) {
        return builder()
                .origin(origin)
                .maxDistance(maxDistance)
                .raySize(raySize)
                .piercing(false)
                .build();
    }

    /**
     * Creates a piercing ray trace selector (hits all entities along the ray).
     *
     * @param origin      the origin and direction of the ray
     * @param maxDistance the maximum ray distance
     * @return a new piercing RayTraceEntitySelector
     */
    public static RayTraceEntitySelector piercing(SelectorOrigin origin, double maxDistance) {
        return builder()
                .origin(origin)
                .maxDistance(maxDistance)
                .piercing(true)
                .build();
    }

    /**
     * Creates a piercing ray trace selector with ray thickness.
     *
     * @param origin      the origin and direction of the ray
     * @param maxDistance the maximum ray distance
     * @param raySize     the thickness of the ray
     * @return a new piercing RayTraceEntitySelector
     */
    public static RayTraceEntitySelector piercing(SelectorOrigin origin, double maxDistance, double raySize) {
        return builder()
                .origin(origin)
                .maxDistance(maxDistance)
                .raySize(raySize)
                .piercing(true)
                .build();
    }

    /**
     * Creates a ray trace selector that respects block collisions.
     *
     * @param origin      the origin and direction of the ray
     * @param maxDistance the maximum ray distance
     * @param piercing    whether the ray should pierce through entities
     * @return a new RayTraceEntitySelector that stops at blocks
     */
    public static RayTraceEntitySelector withBlockCollision(SelectorOrigin origin, double maxDistance, boolean piercing) {
        return builder()
                .origin(origin)
                .maxDistance(maxDistance)
                .piercing(piercing)
                .ignoreBlocks(false)
                .build();
    }

    /**
     * Gets the end position of the ray.
     * Useful for visual effects or debugging.
     *
     * @return the location at the end of the ray
     */
    public Location getRayEndLocation() {
        Location start = origin.toLocation();
        Vector dir = direction != null ? direction.clone().normalize() : origin.getOrientation();
        if (dir == null) {
            return start;
        }
        return start.clone().add(dir.multiply(maxDistance));
    }

    /**
     * Gets the result of the ray trace including hit position information.
     * This is useful when you need more details than just the entity list.
     *
     * @return the ray trace result, or null if nothing was hit
     */
    @Nullable
    public RayTraceResult getRayTraceResult() {
        Location startLocation = origin.toLocation();
        World world = startLocation.getWorld();
        if (world == null) {
            return null;
        }

        Vector rayDirection = direction != null ? direction.clone().normalize() : origin.getOrientation();
        if (rayDirection == null || rayDirection.lengthSquared() < 1e-6) {
            return null;
        }

        Predicate<Entity> entityPredicate = entity -> {
            if (!(entity instanceof LivingEntity living)) {
                return false;
            }
            return filter.test(origin, living);
        };

        if (ignoreBlocks) {
            return world.rayTraceEntities(startLocation, rayDirection, maxDistance, raySize, entityPredicate);
        } else {
            return world.rayTrace(startLocation, rayDirection, maxDistance, fluidCollisionMode, true, raySize, entityPredicate);
        }
    }
}
