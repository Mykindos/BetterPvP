package me.mykindos.betterpvp.core.utilities.model.selector.shape.impl;

import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.RotatableShape;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * An oriented box shape defined by half-extents.
 * The box is centered on the origin and can be rotated.
 * <p>
 * Half-extents define the distance from center to face in each axis:
 * - halfWidth (X axis): left/right extent
 * - halfHeight (Y axis): up/down extent
 * - halfDepth (Z axis): forward/backward extent
 *
 * @param halfWidth       the half-width (X axis extent)
 * @param halfHeight      the half-height (Y axis extent)
 * @param halfDepth       the half-depth (Z axis extent)
 * @param additionalPitch the additional pitch rotation in degrees
 * @param additionalYaw   the additional yaw rotation in degrees
 */
public record BoxShape(
        double halfWidth,
        double halfHeight,
        double halfDepth,
        float additionalPitch,
        float additionalYaw
) implements RotatableShape {

    public BoxShape(double halfWidth, double halfHeight, double halfDepth) {
        this(halfWidth, halfHeight, halfDepth, 0f, 0f);
    }

    public BoxShape {
        if (halfWidth <= 0 || halfHeight <= 0 || halfDepth <= 0) {
            throw new IllegalArgumentException("Half-extents must be positive");
        }
    }

    @Override
    public boolean contains(SelectorOrigin origin, Vector point) {
        Vector originPos = origin.getPosition();

        // Get the relative position of the point from the origin
        Vector relative = point.clone().subtract(originPos);

        // Calculate total rotation (origin orientation + additional)
        float totalPitch = origin.getPitch() + additionalPitch;
        float totalYaw = origin.getYaw() + additionalYaw;

        // Apply inverse rotation to transform point into local box space
        // First rotate around Y axis (yaw), then around X axis (pitch)
        Vector local = rotateInverse(relative, totalPitch, totalYaw);

        // Check if point is within axis-aligned bounds
        return Math.abs(local.getX()) <= halfWidth
                && Math.abs(local.getY()) <= halfHeight
                && Math.abs(local.getZ()) <= halfDepth;
    }

    @Override
    public boolean intersects(SelectorOrigin origin, BoundingBox box) {
        Vector originPos = origin.getPosition();
        float totalPitch = origin.getPitch() + additionalPitch;
        float totalYaw = origin.getYaw() + additionalYaw;

        // Get the rotated box's axes in world space
        Vector[] obbAxes = getRotatedAxes(totalPitch, totalYaw);
        double[] obbHalfExtents = {halfWidth, halfHeight, halfDepth};

        // AABB center and half-extents
        Vector aabbCenter = box.getCenter();
        Vector aabbHalfExtents = new Vector(
                (box.getMaxX() - box.getMinX()) / 2.0,
                (box.getMaxY() - box.getMinY()) / 2.0,
                (box.getMaxZ() - box.getMinZ()) / 2.0
        );

        // Vector from OBB center to AABB center
        Vector centerDiff = aabbCenter.clone().subtract(originPos);

        // Use Separating Axis Theorem (SAT)
        // Test 15 axes: 3 from AABB, 3 from OBB, 9 cross products

        // AABB axes (world axes)
        Vector[] aabbAxes = {
                new Vector(1, 0, 0),
                new Vector(0, 1, 0),
                new Vector(0, 0, 1)
        };

        // Test AABB axes
        for (int i = 0; i < 3; i++) {
            if (isSeparatingAxis(aabbAxes[i], centerDiff, obbAxes, obbHalfExtents, aabbHalfExtents)) {
                return false;
            }
        }

        // Test OBB axes
        for (int i = 0; i < 3; i++) {
            if (isSeparatingAxis(obbAxes[i], centerDiff, obbAxes, obbHalfExtents, aabbHalfExtents)) {
                return false;
            }
        }

        // Test cross products of axes
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vector cross = aabbAxes[i].clone().crossProduct(obbAxes[j]);
                if (cross.lengthSquared() > 1e-10) { // Skip near-parallel axes
                    if (isSeparatingAxis(cross.normalize(), centerDiff, obbAxes, obbHalfExtents, aabbHalfExtents)) {
                        return false;
                    }
                }
            }
        }

        // No separating axis found - boxes intersect
        return true;
    }

    /**
     * Tests if an axis is a separating axis between the two boxes.
     */
    private boolean isSeparatingAxis(Vector axis, Vector centerDiff,
                                     Vector[] obbAxes, double[] obbHalfExtents,
                                     Vector aabbHalfExtents) {
        // Project center difference onto axis
        double centerProjection = Math.abs(centerDiff.dot(axis));

        // Project OBB half-extents onto axis
        double obbProjection = 0;
        for (int i = 0; i < 3; i++) {
            obbProjection += obbHalfExtents[i] * Math.abs(obbAxes[i].dot(axis));
        }

        // Project AABB half-extents onto axis
        double aabbProjection = aabbHalfExtents.getX() * Math.abs(axis.getX())
                + aabbHalfExtents.getY() * Math.abs(axis.getY())
                + aabbHalfExtents.getZ() * Math.abs(axis.getZ());

        // If projections don't overlap, this is a separating axis
        return centerProjection > obbProjection + aabbProjection;
    }

    /**
     * Gets the rotated box's local axes in world space.
     */
    private Vector[] getRotatedAxes(float pitch, float yaw) {
        // Local axes before rotation
        Vector localX = new Vector(1, 0, 0);
        Vector localY = new Vector(0, 1, 0);
        Vector localZ = new Vector(0, 0, 1);

        // Apply rotation
        return new Vector[]{
                rotateForward(localX, pitch, yaw),
                rotateForward(localY, pitch, yaw),
                rotateForward(localZ, pitch, yaw)
        };
    }

    /**
     * Applies forward rotation to transform local-space vector to world space.
     */
    private Vector rotateForward(Vector v, float pitch, float yaw) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        // Pitch rotation (around X axis)
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        double y1 = v.getY() * cosPitch - v.getZ() * sinPitch;
        double z1 = v.getY() * sinPitch + v.getZ() * cosPitch;
        double x1 = v.getX();

        // Yaw rotation (around Y axis)
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double x2 = x1 * cosYaw - z1 * sinYaw;
        double z2 = x1 * sinYaw + z1 * cosYaw;

        return new Vector(x2, y1, z2);
    }

    /**
     * Applies inverse rotation to transform a world-space vector into local box space.
     */
    private Vector rotateInverse(Vector v, float pitch, float yaw) {
        // Convert to radians
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        // Inverse yaw rotation (around Y axis)
        double cosYaw = Math.cos(-yawRad);
        double sinYaw = Math.sin(-yawRad);
        double x1 = v.getX() * cosYaw - v.getZ() * sinYaw;
        double z1 = v.getX() * sinYaw + v.getZ() * cosYaw;
        double y1 = v.getY();

        // Inverse pitch rotation (around X axis)
        double cosPitch = Math.cos(-pitchRad);
        double sinPitch = Math.sin(-pitchRad);
        double y2 = y1 * cosPitch - z1 * sinPitch;
        double z2 = y1 * sinPitch + z1 * cosPitch;

        return new Vector(x1, y2, z2);
    }

    @Override
    public double getBoundingRadius() {
        // Return the diagonal distance from center to corner
        return Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight + halfDepth * halfDepth);
    }

    @Override
    public RotatableShape withRotation(float pitch, float yaw) {
        return new BoxShape(halfWidth, halfHeight, halfDepth, pitch, yaw);
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
