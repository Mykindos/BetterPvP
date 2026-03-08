package me.mykindos.betterpvp.core.utilities.model.selector.shape.impl;

import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.RotatableShape;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * A cylinder shape that can be tilted using rotation.
 * The default axis is Y-up (vertical cylinder). Rotation tilts the axis.
 * <p>
 * Parameters:
 * - radius: The radius of the cylinder
 * - halfHeight: The half-height (distance from center to top/bottom)
 *
 * @param radius          the radius of the cylinder
 * @param halfHeight      the half-height of the cylinder
 * @param additionalPitch the additional pitch rotation in degrees (tilts the axis)
 * @param additionalYaw   the additional yaw rotation in degrees (tilts the axis)
 */
public record CylinderShape(
        double radius,
        double halfHeight,
        float additionalPitch,
        float additionalYaw
) implements RotatableShape {

    public CylinderShape(double radius, double halfHeight) {
        this(radius, halfHeight, 0f, 0f);
    }

    public CylinderShape {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        if (halfHeight <= 0) {
            throw new IllegalArgumentException("Half-height must be positive");
        }
    }

    @Override
    public boolean contains(SelectorOrigin origin, Vector point) {
        Vector originPos = origin.getPosition();
        Vector relative = point.clone().subtract(originPos);

        // Get the cylinder's axis direction
        Vector axis = getAxisDirection(origin);

        // Project the relative vector onto the axis to get height
        double heightOnAxis = relative.dot(axis);

        // Check if within height bounds
        if (Math.abs(heightOnAxis) > halfHeight) {
            return false;
        }

        // Calculate the perpendicular distance from the axis
        Vector alongAxis = axis.clone().multiply(heightOnAxis);
        Vector perpendicular = relative.clone().subtract(alongAxis);
        double perpendicularDistance = perpendicular.length();

        // Check if within radius
        return perpendicularDistance <= radius;
    }

    @Override
    public boolean intersects(SelectorOrigin origin, BoundingBox box) {
        Vector originPos = origin.getPosition();
        Vector axis = getAxisDirection(origin);

        // The cylinder's axis segment endpoints
        Vector axisStart = originPos.clone().subtract(axis.clone().multiply(halfHeight));
        Vector axisEnd = originPos.clone().add(axis.clone().multiply(halfHeight));

        // Find the closest point on the AABB to the cylinder's axis line segment
        Vector closestOnBox = closestPointOnBoxToLineSegment(box, axisStart, axisEnd);

        // Now check if this point is within the cylinder
        Vector relative = closestOnBox.clone().subtract(originPos);
        double heightOnAxis = relative.dot(axis);

        // Clamp height to cylinder bounds
        double clampedHeight = Math.max(-halfHeight, Math.min(halfHeight, heightOnAxis));

        // Find the closest point on the axis segment to the box point
        Vector closestOnAxis = originPos.clone().add(axis.clone().multiply(clampedHeight));

        // Check if the distance is within radius
        double distanceSquared = closestOnBox.distanceSquared(closestOnAxis);
        return distanceSquared <= radius * radius;
    }

    /**
     * Finds the closest point on an AABB to a line segment.
     */
    private Vector closestPointOnBoxToLineSegment(BoundingBox box, Vector segStart, Vector segEnd) {
        // We need to find the point on the box that is closest to the line segment
        // This is done by finding the closest point on the segment to the box,
        // then clamping that to the box bounds

        Vector segDir = segEnd.clone().subtract(segStart);
        double segLengthSq = segDir.lengthSquared();

        if (segLengthSq < 1e-10) {
            // Degenerate segment (point)
            return clampToBox(box, segStart);
        }

        // Find the closest point on the infinite line to the box center
        Vector boxCenter = box.getCenter();
        double t = segDir.dot(boxCenter.clone().subtract(segStart)) / segLengthSq;
        t = Math.max(0, Math.min(1, t));

        // Point on segment closest to box center
        Vector pointOnSeg = segStart.clone().add(segDir.clone().multiply(t));

        // Now find closest point on box to this segment point
        Vector closestOnBox = clampToBox(box, pointOnSeg);

        // Iterate to refine: find point on segment closest to closestOnBox
        t = segDir.dot(closestOnBox.clone().subtract(segStart)) / segLengthSq;
        t = Math.max(0, Math.min(1, t));
        pointOnSeg = segStart.clone().add(segDir.clone().multiply(t));

        // Final clamp to box
        return clampToBox(box, pointOnSeg);
    }

    /**
     * Clamps a point to the nearest point on or inside the bounding box.
     */
    private Vector clampToBox(BoundingBox box, Vector point) {
        return new Vector(
                Math.max(box.getMinX(), Math.min(point.getX(), box.getMaxX())),
                Math.max(box.getMinY(), Math.min(point.getY(), box.getMaxY())),
                Math.max(box.getMinZ(), Math.min(point.getZ(), box.getMaxZ()))
        );
    }

    /**
     * Calculates the axis direction of the cylinder.
     * Default is Y-up, but rotation can tilt it.
     */
    private Vector getAxisDirection(SelectorOrigin origin) {
        // Start with Y-up axis
        Vector axis = new Vector(0, 1, 0);

        // Apply rotation if any
        float totalPitch = origin.getPitch() + additionalPitch;
        float totalYaw = origin.getYaw() + additionalYaw;

        if (totalPitch != 0 || totalYaw != 0) {
            // Rotate the axis by pitch and yaw
            double pitchRad = Math.toRadians(totalPitch);
            double yawRad = Math.toRadians(totalYaw);

            // Rotate around X axis (pitch)
            double y1 = axis.getY() * Math.cos(pitchRad) - axis.getZ() * Math.sin(pitchRad);
            double z1 = axis.getY() * Math.sin(pitchRad) + axis.getZ() * Math.cos(pitchRad);
            double x1 = axis.getX();

            // Rotate around Y axis (yaw)
            double x2 = x1 * Math.cos(yawRad) + z1 * Math.sin(yawRad);
            double z2 = -x1 * Math.sin(yawRad) + z1 * Math.cos(yawRad);

            axis = new Vector(x2, y1, z2).normalize();
        }

        return axis;
    }

    @Override
    public double getBoundingRadius() {
        // The bounding radius is the distance from center to the farthest corner
        return Math.sqrt(radius * radius + halfHeight * halfHeight);
    }

    @Override
    public RotatableShape withRotation(float pitch, float yaw) {
        return new CylinderShape(radius, halfHeight, pitch, yaw);
    }

    @Override
    public float getAdditionalPitch() {
        return additionalPitch;
    }

    @Override
    public float getAdditionalYaw() {
        return additionalYaw;
    }
}
