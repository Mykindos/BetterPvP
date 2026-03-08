package me.mykindos.betterpvp.core.utilities.model.selector.shape.impl;

import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.Shape;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * A spherical shape defined by a radius.
 * Spheres are rotationally symmetric, so no rotation support is needed.
 *
 * @param radius the radius of the sphere
 */
public record SphereShape(double radius) implements Shape {

    public SphereShape {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
    }

    @Override
    public boolean contains(SelectorOrigin origin, Vector point) {
        Vector originPos = origin.getPosition();
        double distanceSquared = originPos.distanceSquared(point);
        return distanceSquared <= radius * radius;
    }

    @Override
    public boolean intersects(SelectorOrigin origin, BoundingBox box) {
        Vector center = origin.getPosition();

        // Find the closest point on the AABB to the sphere center
        double closestX = Math.max(box.getMinX(), Math.min(center.getX(), box.getMaxX()));
        double closestY = Math.max(box.getMinY(), Math.min(center.getY(), box.getMaxY()));
        double closestZ = Math.max(box.getMinZ(), Math.min(center.getZ(), box.getMaxZ()));

        // Check if the closest point is within the sphere's radius
        double dx = closestX - center.getX();
        double dy = closestY - center.getY();
        double dz = closestZ - center.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        return distanceSquared <= radius * radius;
    }

    @Override
    public double getBoundingRadius() {
        return radius;
    }
}
