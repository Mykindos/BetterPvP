package me.mykindos.betterpvp.core.utilities.model.selector.shape;

import lombok.Builder;
import lombok.Value;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntityFilter;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntityFilters;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntitySelector;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.impl.ArcShape;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.impl.BoxShape;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.impl.CylinderShape;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.impl.SphereShape;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * The main implementation for shape-based entity selection.
 * Combines a shape with filters to select entities within the shape that pass the filter criteria.
 * <p>
 * Usage examples:
 * <pre>
 * // Sphere around player
 * ShapeEntitySelector.sphere(new EntityOrigin(player), 5.0)
 *     .withFilter(EntityFilters.combatEnemies())
 *     .select();
 *
 * // Frontal cone attack
 * ShapeEntitySelector.arc(new EntityOrigin(player, true), 8.0, 90.0, 45.0)
 *     .withFilter(EntityFilters.combatEnemies())
 *     .select();
 *
 * // Rotated box
 * ShapeEntitySelector.builder()
 *     .origin(new EntityOrigin(player))
 *     .shape(new BoxShape(2, 1, 3).withRotation(0, 45))
 *     .filter(EntityFilters.combat())
 *     .build()
 *     .select();
 * </pre>
 */
@Value
@Builder(toBuilder = true)
public class ShapeEntitySelector implements EntitySelector<LivingEntity> {

    /**
     * Buffer added to bounding radius to account for entity height and large hitboxes.
     * Entities are fetched by their feet position, but we need to include entities
     * whose hitbox extends into the selection area from further away.
     */
    private static final double ENTITY_FETCH_BUFFER = 3.0;

    /**
     * The origin point for the selection.
     */
    SelectorOrigin origin;

    /**
     * The shape that defines the selection area.
     */
    Shape shape;

    /**
     * The filter to apply to potential entities.
     * Defaults to the standard combat filter.
     */
    @Builder.Default
    EntityFilter filter = EntityFilters.combat();

    @Override
    public Collection<LivingEntity> select() {
        Location loc = origin.toLocation();
        // Add buffer to catch entities whose feet are further but hitbox extends into range
        double boundingRadius = shape.getBoundingRadius() + ENTITY_FETCH_BUFFER;

        // Gather all nearby living entities within the expanded bounding radius
        return UtilLocation.getNearbyLivingEntities(loc, boundingRadius).stream()
                // Filter by the entity filter
                .filter(entity -> filter.test(origin, entity))
                // Filter by precise bounding box intersection with the shape
                .filter(entity -> shape.intersects(origin, entity.getBoundingBox()))
                .collect(Collectors.toList());
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
     * Creates a spherical selector centered on the origin.
     *
     * @param origin the center of the sphere
     * @param radius the radius of the sphere
     * @return a new ShapeEntitySelector with a sphere shape
     */
    public static ShapeEntitySelector sphere(SelectorOrigin origin, double radius) {
        return builder()
                .origin(origin)
                .shape(new SphereShape(radius))
                .build();
    }

    /**
     * Creates a box selector centered on the origin.
     *
     * @param origin     the center of the box
     * @param halfWidth  the half-width (X axis)
     * @param halfHeight the half-height (Y axis)
     * @param halfDepth  the half-depth (Z axis)
     * @return a new ShapeEntitySelector with a box shape
     */
    public static ShapeEntitySelector box(SelectorOrigin origin, double halfWidth, double halfHeight, double halfDepth) {
        return builder()
                .origin(origin)
                .shape(new BoxShape(halfWidth, halfHeight, halfDepth))
                .build();
    }

    /**
     * Creates an arc/cone selector extending from the origin in its facing direction.
     *
     * @param origin          the origin and direction of the arc
     * @param radius          the maximum reach of the arc
     * @param horizontalAngle the total horizontal spread in degrees
     * @param verticalAngle   the total vertical spread in degrees
     * @return a new ShapeEntitySelector with an arc shape
     */
    public static ShapeEntitySelector arc(SelectorOrigin origin, double radius, double horizontalAngle, double verticalAngle) {
        return builder()
                .origin(origin)
                .shape(new ArcShape(radius, horizontalAngle, verticalAngle))
                .build();
    }

    /**
     * Creates a cylindrical selector centered on the origin.
     * The cylinder's axis is vertical by default.
     *
     * @param origin     the center of the cylinder
     * @param radius     the radius of the cylinder
     * @param halfHeight the half-height of the cylinder
     * @return a new ShapeEntitySelector with a cylinder shape
     */
    public static ShapeEntitySelector cylinder(SelectorOrigin origin, double radius, double halfHeight) {
        return builder()
                .origin(origin)
                .shape(new CylinderShape(radius, halfHeight))
                .build();
    }
}
