package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.Optional;

public class UtilLocation {

    /**
     * Get a {@link Location} in a fixed direction from a reference at a specified distance.
     * The angle should be in degrees, where 0 is north, 90 is east, 180 is south, and 270 is west.
     *
     * @param reference The reference location
     * @param radius    The distance from the reference location
     * @param degree    The direction from the reference location
     * @return The new location
     */
    public static Location fromFixedAngleDistance(final Location reference, final double radius, final double degree) {
        final Location north = reference.clone().setDirection(BlockFace.NORTH.getDirection());
        return fromAngleDistance(north, radius, degree);
    }

    /**
     * Get a {@link Location} in a direction from a reference at a specified distance.
     * The angle should be in degrees, where 0 is the direction of the reference location as {@link Vector}.
     *
     * @param reference The reference location
     * @param radius    The distance from the reference location
     * @param degree    The direction from the reference location
     * @return The new location
     */
    public static Location fromAngleDistance(final Location reference, final double radius, final double degree) {
        Preconditions.checkArgument(radius > 0, "Radius must be greater than 0");
        final Vector direction = reference.getDirection(); // unit-vector in the direction of the reference location
        direction.multiply(radius); // multiply by the radius to get a full-length vector in the direction of the reference
        direction.rotateAroundY(Math.toRadians(degree)); // rotate the vector around the y-axis by the specified angle
        return reference.clone().add(direction);
    }


    /**
     * Get the closest surface block relative to a {@link Location}.
     *
     * @param location The location to scan from
     * @param keepXZ   Whether to keep the x and z coordinates of the location. If false, thee x and z coordinates will be those of the block
     * @return The closest surface block, or {@link Optional#empty()} if none was found
     * @see #getClosestSurfaceBlock(Location, double, boolean)
     */
    public static Optional<Block> getClosestSurfaceBlock(final Location location, final boolean keepXZ) {
        return getClosestSurfaceBlock(location, location.getWorld().getMaxHeight(), keepXZ).map(Location::getBlock);
    }

    /**
     * Get the closest surface block relative to a {@link Location}. The location is scanned from its height down until it finds
     * a surface block, or from its height up until it finds a surface block.
     * <p>
     * A surface block is a block that does not have a <b>solid</b> block above it, and is relative to the center location's height.
     *
     * @param location            The location to scan from
     * @param maxHeightDifference The maximum height difference to scan the block from
     * @param keepXZ              Whether to keep the x and z coordinates of the location. If false, thee x and z coordinates will be those of the block
     * @return The closest surface block, or {@link Optional#empty()} if none was found
     */
    public static Optional<Location> getClosestSurfaceBlock(final Location location, final double maxHeightDifference, final boolean keepXZ) {
        Preconditions.checkState(maxHeightDifference > 0, "Max height difference must be greater than 0");
        final int y = location.getBlockY();
        Block block = location.getBlock();
        while (UtilBlock.solid(block)) { // Replacing the solid block with the one above it until we hit a non-solid block
            block = block.getRelative(BlockFace.UP);
            if (Math.abs(y - block.getY()) > maxHeightDifference) {
                return Optional.empty(); // If the block is too high, there wasn't a surface block
            }
        }
        // At this point, we have the highest non-solid block closest to the block. This could mean we already looped to the first non-solid
        // or we never looped at all because the location was already non-solid, which means we need to loop down to find the first solid
        while (!UtilBlock.solid(block.getRelative(BlockFace.DOWN))) {
            block = block.getRelative(BlockFace.DOWN);
            if (Math.abs(y - block.getY()) > maxHeightDifference) {
                return Optional.empty(); // If the block is too low, means there were no surface-blocks relative to the location
            }
        }
        block = block.getRelative(BlockFace.DOWN); // Replace the non-solid block with the one below it
        final Location result = block.getLocation().clone();
        if (keepXZ) {
            result.setX(location.getX());
            result.setZ(location.getZ());
        }
        return Optional.of(result);
    }

}
