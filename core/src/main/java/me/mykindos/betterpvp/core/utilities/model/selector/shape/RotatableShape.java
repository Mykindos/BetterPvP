package me.mykindos.betterpvp.core.utilities.model.selector.shape;

/**
 * A shape that supports additional rotation on top of the origin's orientation.
 * This allows shapes to be rotated relative to the direction the origin is facing.
 */
public interface RotatableShape extends Shape {

    /**
     * Creates a new shape with the specified additional rotation.
     * This rotation is applied on top of the origin's orientation.
     *
     * @param pitch the additional pitch rotation in degrees
     * @param yaw   the additional yaw rotation in degrees
     * @return a new shape with the additional rotation applied
     */
    RotatableShape withRotation(float pitch, float yaw);

    /**
     * Gets the additional pitch rotation of this shape.
     *
     * @return the additional pitch in degrees
     */
    float getAdditionalPitch();

    /**
     * Gets the additional yaw rotation of this shape.
     *
     * @return the additional yaw in degrees
     */
    float getAdditionalYaw();
}
