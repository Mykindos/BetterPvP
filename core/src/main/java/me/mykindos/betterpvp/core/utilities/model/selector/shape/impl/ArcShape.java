package me.mykindos.betterpvp.core.utilities.model.selector.shape.impl;

import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.RotatableShape;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * An arc/cone shape useful for frontal attacks and fan-shaped abilities.
 * The arc extends from the origin in the direction it's facing.
 * <p>
 * Parameters:
 * - radius: The maximum reach of the arc
 * - horizontalAngle: The total horizontal spread in degrees (centered on facing direction)
 * - verticalAngle: The total vertical spread in degrees (centered on facing direction)
 *
 * @param radius          the maximum reach of the arc
 * @param horizontalAngle the total horizontal spread in degrees
 * @param verticalAngle   the total vertical spread in degrees
 * @param additionalPitch the additional pitch rotation in degrees
 * @param additionalYaw   the additional yaw rotation in degrees
 */
public record ArcShape(
        double radius,
        double horizontalAngle,
        double verticalAngle,
        float additionalPitch,
        float additionalYaw
) implements RotatableShape {

    public ArcShape(double radius, double horizontalAngle, double verticalAngle) {
        this(radius, horizontalAngle, verticalAngle, 0f, 0f);
    }

    public ArcShape {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        if (horizontalAngle <= 0 || horizontalAngle > 360) {
            throw new IllegalArgumentException("Horizontal angle must be between 0 and 360 degrees");
        }
        if (verticalAngle <= 0 || verticalAngle > 180) {
            throw new IllegalArgumentException("Vertical angle must be between 0 and 180 degrees");
        }
    }

    @Override
    public boolean contains(SelectorOrigin origin, Vector point) {
        Vector originPos = origin.getPosition();
        Vector toPoint = point.clone().subtract(originPos);
        double distance = toPoint.length();

        // Check if within radius
        if (distance > radius || distance < 1e-6) {
            return distance < 1e-6; // Point at origin is always in arc
        }

        return isDirectionWithinArc(origin, toPoint.normalize());
    }

    @Override
    public boolean intersects(SelectorOrigin origin, BoundingBox box) {
        Vector originPos = origin.getPosition();
        Vector forward = getForwardDirection(origin);

        // Check 1: Does a ray in the forward direction hit the box within radius?
        RayTraceResult forwardHit = box.rayTrace(originPos, forward, radius);
        if (forwardHit != null) {
            return true; // Direct hit in look direction
        }

        // Check 2: Are any of the box's corners within the arc?
        Vector[] corners = getBoxCorners(box);
        for (Vector corner : corners) {
            if (contains(origin, corner)) {
                return true;
            }
        }

        // Check 3: Sample points along box edges for edge-crossing detection
        // This catches cases where the arc boundary crosses through a box edge
        for (int i = 0; i < corners.length; i++) {
            for (int j = i + 1; j < corners.length; j++) {
                // Only check edges (corners differing in exactly one axis)
                Vector c1 = corners[i];
                Vector c2 = corners[j];
                int diffCount = 0;
                if (c1.getX() != c2.getX()) diffCount++;
                if (c1.getY() != c2.getY()) diffCount++;
                if (c1.getZ() != c2.getZ()) diffCount++;

                if (diffCount == 1) {
                    // This is an edge, sample it
                    if (edgeIntersectsArc(origin, c1, c2)) {
                        return true;
                    }
                }
            }
        }

        // Check 4: Ray trace from origin toward box center and face centers
        // This catches cases where the box occludes the arc's view
        Vector[] checkPoints = getBoxCheckPoints(box);
        for (Vector point : checkPoints) {
            Vector toPoint = point.clone().subtract(originPos);
            double distance = toPoint.length();

            if (distance < 1e-6 || distance > radius) {
                continue;
            }

            Vector direction = toPoint.normalize();

            // Check if this direction is within our angle constraints
            if (isDirectionWithinArc(origin, direction)) {
                // Verify the ray actually hits the box
                RayTraceResult hit = box.rayTrace(originPos, direction, radius);
                if (hit != null) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if an edge between two corners intersects the arc.
     * Uses binary search to find potential intersection points.
     */
    private boolean edgeIntersectsArc(SelectorOrigin origin, Vector c1, Vector c2) {
        // Sample points along the edge
        int samples = 8;
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            Vector point = c1.clone().multiply(1 - t).add(c2.clone().multiply(t));
            if (contains(origin, point)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a direction vector is within the arc's angle constraints.
     */
    private boolean isDirectionWithinArc(SelectorOrigin origin, Vector direction) {
        Vector forward = getForwardDirection(origin);

        // For a full 3D cone, we just check the total angle
        if (horizontalAngle >= 360 && verticalAngle >= 180) {
            return true; // Full sphere
        }

        // For symmetric cones with equal horizontal and vertical angles
        if (Math.abs(horizontalAngle - verticalAngle * 2) < 1e-6) {
            double angle = Math.toDegrees(forward.angle(direction));
            return angle <= horizontalAngle / 2.0;
        }

        // For asymmetric arcs, check horizontal and vertical separately
        // Project onto the horizontal plane (XZ)
        Vector horizontalForward = new Vector(forward.getX(), 0, forward.getZ());
        Vector horizontalDir = new Vector(direction.getX(), 0, direction.getZ());

        if (horizontalForward.lengthSquared() > 1e-6 && horizontalDir.lengthSquared() > 1e-6) {
            horizontalForward.normalize();
            horizontalDir.normalize();
            double hAngle = Math.toDegrees(horizontalForward.angle(horizontalDir));
            if (hAngle > horizontalAngle / 2.0) {
                return false;
            }
        }

        // Check vertical angle
        double dirVertical = direction.getY();
        double forwardVertical = forward.getY();
        double pointVerticalAngle = Math.toDegrees(Math.asin(Math.abs(dirVertical - forwardVertical)));
        return pointVerticalAngle <= verticalAngle / 2.0;
    }

    /**
     * Gets the 8 corners of a bounding box.
     */
    private Vector[] getBoxCorners(BoundingBox box) {
        return new Vector[]{
                new Vector(box.getMinX(), box.getMinY(), box.getMinZ()),
                new Vector(box.getMinX(), box.getMinY(), box.getMaxZ()),
                new Vector(box.getMinX(), box.getMaxY(), box.getMinZ()),
                new Vector(box.getMinX(), box.getMaxY(), box.getMaxZ()),
                new Vector(box.getMaxX(), box.getMinY(), box.getMinZ()),
                new Vector(box.getMaxX(), box.getMinY(), box.getMaxZ()),
                new Vector(box.getMaxX(), box.getMaxY(), box.getMinZ()),
                new Vector(box.getMaxX(), box.getMaxY(), box.getMaxZ())
        };
    }

    /**
     * Gets strategic check points on the box (center and face centers).
     */
    private Vector[] getBoxCheckPoints(BoundingBox box) {
        Vector center = box.getCenter();
        double midX = center.getX();
        double midY = center.getY();
        double midZ = center.getZ();

        return new Vector[]{
                center,
                // Face centers
                new Vector(box.getMinX(), midY, midZ),
                new Vector(box.getMaxX(), midY, midZ),
                new Vector(midX, box.getMinY(), midZ),
                new Vector(midX, box.getMaxY(), midZ),
                new Vector(midX, midY, box.getMinZ()),
                new Vector(midX, midY, box.getMaxZ())
        };
    }

    /**
     * Calculates the forward direction including origin orientation and additional rotation.
     */
    private Vector getForwardDirection(SelectorOrigin origin) {
        float totalPitch = origin.getPitch() + additionalPitch;
        float totalYaw = origin.getYaw() + additionalYaw;

        // Convert pitch and yaw to direction vector
        double pitchRad = Math.toRadians(totalPitch);
        double yawRad = Math.toRadians(totalYaw);

        double xz = Math.cos(pitchRad);
        return new Vector(
                -xz * Math.sin(yawRad),
                -Math.sin(pitchRad),
                xz * Math.cos(yawRad)
        );
    }

    @Override
    public double getBoundingRadius() {
        return radius;
    }

    @Override
    public RotatableShape withRotation(float pitch, float yaw) {
        return new ArcShape(radius, horizontalAngle, verticalAngle, pitch, yaw);
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
