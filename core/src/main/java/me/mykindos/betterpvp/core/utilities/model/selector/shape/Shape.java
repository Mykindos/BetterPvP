package me.mykindos.betterpvp.core.utilities.model.selector.shape;

import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Represents a geometric shape that can be used for entity selection.
 * Shapes define containment logic for determining if a point is inside the shape.
 */
public interface Shape {

    /**
     * Checks if a point is contained within this shape.
     * The origin provides the position and orientation reference for the shape.
     *
     * @param origin the origin of the shape
     * @param point  the point to check (world coordinates)
     * @return true if the point is inside the shape, false otherwise
     */
    boolean contains(SelectorOrigin origin, Vector point);

    /**
     * Checks if a bounding box intersects with this shape.
     * This provides precise hitbox detection for entity selection.
     *
     * @param origin the origin of the shape
     * @param box    the bounding box to check (world coordinates)
     * @return true if any part of the bounding box intersects the shape, false otherwise
     */
    boolean intersects(SelectorOrigin origin, BoundingBox box);

    /**
     * Gets the bounding radius of this shape.
     * This is used for initial entity gathering before precise containment checks.
     * Should return a value large enough to encompass all possible containment points.
     *
     * @return the bounding radius
     */
    double getBoundingRadius();
}
