package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilLocation {

    public static final float DEFAULT_FOV = 73f;

    public static Set<Location> getBoundingBoxCorners(final World world, final BoundingBox boundingBox) {
        return new HashSet<>(Arrays.asList(
                new Location(world, boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ()),
                new Location(world, boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMaxZ()),
                new Location(world, boundingBox.getMaxX(), boundingBox.getMinY(), boundingBox.getMinZ()),
                new Location(world, boundingBox.getMaxX(), boundingBox.getMinY(), boundingBox.getMaxZ()),
                new Location(world, boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMinZ()),
                new Location(world, boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMaxZ()),
                new Location(world, boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMinZ()),
                new Location(world, boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ())
        ));
    }

    public static void drawBoundingBox(BoundingBox box) {
        final double minX = box.getMinX();
        final double minY = box.getMinY();
        final double minZ = box.getMinZ();
        final double maxX = box.getMaxX();
        final double maxY = box.getMaxY();
        final double maxZ = box.getMaxZ();
        final World world = Bukkit.getWorlds().get(0);

        final Location maxMaxMax = new Vector(maxX, maxY, maxZ).toLocation(world);
        final Location maxMaxMin = new Vector(maxX, maxY, minZ).toLocation(world);
        final Location maxMinMax = new Vector(maxX, minY, maxZ).toLocation(world);
        final Location maxMinMin = new Vector(maxX, minY, minZ).toLocation(world);
        final Location minMaxMax = new Vector(minX, maxY, maxZ).toLocation(world);
        final Location minMaxMin = new Vector(minX, maxY, minZ).toLocation(world);
        final Location minMinMax = new Vector(minX, minY, maxZ).toLocation(world);
        final Location minMinMin = new Vector(minX, minY, minZ).toLocation(world);

        drawBetween(maxMaxMax, maxMaxMin);
        drawBetween(maxMaxMax, maxMinMax);
        drawBetween(maxMaxMax, minMaxMax);
        drawBetween(maxMaxMin, maxMinMin);
        drawBetween(maxMaxMin, minMaxMin);
        drawBetween(maxMinMax, maxMinMin);
        drawBetween(maxMinMax, minMinMax);
        drawBetween(minMaxMax, minMaxMin);
        drawBetween(minMaxMax, minMinMax);
        drawBetween(minMaxMin, minMinMin);
        drawBetween(minMinMax, minMinMin);
        drawBetween(maxMinMin, minMinMin);
    }

    public static void drawBetween(Location start, Location end) {
        final VectorLine line = VectorLine.withStepSize(start, end, 0.15);
        for (Location location : line.toLocations()) {
            Particle.REDSTONE.builder().location(location).color(Color.RED).receivers(50).spawn();
        }
    }

    /**
     * Check if a location is in front of an entity's screen, even if there's an obstacle between.
     *
     * @param entity The entity to check.
     * @param other  The location to check.
     * @return Whether the location is in front of the entity.
     */
    public static boolean isInFront(final LivingEntity entity, final Location other) {
        return isInFront(entity, other, DEFAULT_FOV);
    }

    /**
     * Check if a location is in front of an entity's screen, even if there's an obstacle between.
     *
     * @param entity The entity to check.
     * @param other  The location to check.
     * @param angle  The angle of the entity's field of view.
     * @return Whether the location is in front of the entity.
     */
    public static boolean isInFront(final LivingEntity entity, final Location other, final float angle) {
        if (!entity.hasLineOfSight(other)) {
            return false;
        }

        final Vector direction = entity.getEyeLocation().getDirection(); // This is already normalized
        final Vector lineToLocation = other.subtract(entity.getEyeLocation()).toVector().normalize();
        final double angle2 = Math.toDegrees(lineToLocation.angle(direction));
        return Math.abs(angle2) <= angle;
    }

    /**
     * Shift a {@link BoundingBox} located at a {@link Location} out of nearby, possibly colliding blocks.
     *
     * @param boundingBoxFloor The location to shift the bounding box out of
     * @param boundingBox      The bounding box to shift out of blocks
     * @return The nearest location that the bounding box can be at without colliding with blocks
     */
    public static Location shiftOutOfBlocks(Location boundingBoxFloor, final BoundingBox boundingBox) {
        boundingBoxFloor = boundingBoxFloor.clone(); // Clone it because we're going to modify it
        final BoundingBox effectiveAABB = copyAABBToLocation(boundingBox, boundingBoxFloor);

        final World world = boundingBoxFloor.getWorld();
        // check all corners of the player's bounding box and see if they are in a block
        final Set<Block> collidingBlocks = getBoundingBoxCorners(world, effectiveAABB).stream().map(Location::getBlock).collect(Collectors.toSet());
        // add blocks under the player's bounding box
        collidingBlocks.addAll(collidingBlocks.stream().map(block -> block.getRelative(BlockFace.DOWN)).collect(Collectors.toSet()));

        // Remove passable blocks, players can stand in those. Also skip if we're not actually colliding with the block's shape
        collidingBlocks.removeIf(Block::isPassable);

        // Only shift if we're actually colliding with a block
        if (!collidingBlocks.isEmpty()) {
            // Shift the bounding box back by the difference between the player's bounding box and the block's bounding box
            for (final Block collidingBlock : collidingBlocks) {
                final Collection<BoundingBox> boundingBoxes = UtilBlock.getBoundingBoxes(collidingBlock);
                for (final BoundingBox collidingBox : boundingBoxes) {
                    if (!collidingBox.overlaps(effectiveAABB)) {
                        continue; // Skip if the box is not colliding with the block
                    }

                    final Vector blockAABB = new Vector((int) effectiveAABB.getCenterX(), collidingBox.getCenterY(), (int) effectiveAABB.getCenterZ());
                    final Vector directionToMove = blockAABB.clone().subtract(collidingBox.getCenter()).normalize(); // Get the direction to move the player
                    final BoundingBox intersection = collidingBox.clone().intersection(effectiveAABB);

                    double deltaX = Math.abs(intersection.getWidthX()) < Math.abs(intersection.getWidthZ()) ? intersection.getWidthX() : 0f;
                    double deltaZ = Math.abs(intersection.getWidthX()) > Math.abs(intersection.getWidthZ()) ? intersection.getWidthZ() : 0f;
                    effectiveAABB.shift(new Vector(
                            deltaX * Math.signum(directionToMove.getX()),
                            intersection.getHeight() * Math.signum(directionToMove.getY()),
                            deltaZ * Math.signum(directionToMove.getZ())));
                }
            }

            // Set the new location to the out-of-bounds location
            boundingBoxFloor.setX(effectiveAABB.getCenterX());
            boundingBoxFloor.setY(effectiveAABB.getMinY());
            boundingBoxFloor.setZ(effectiveAABB.getCenterZ());
        }

        return boundingBoxFloor;
    }

    /**
     * Copy a {@link BoundingBox} to a {@link Location} and return the new {@link BoundingBox}.
     *
     * @param boundingBox            The bounding box to copy
     * @param boundingBoxFloorCenter The location to copy the bounding box to
     * @return The new bounding box
     */
    public static BoundingBox copyAABBToLocation(final BoundingBox boundingBox, final Location boundingBoxFloorCenter) {
        // Verify that the tp location won't put the player in a block and allow them to phase
        // Perform a raytrace with 0 distance to check if the player is in a block
        return BoundingBox.of(
                boundingBoxFloorCenter.clone().add(0.0, boundingBox.getHeight() / 2, 0.0),
                boundingBox.getWidthX() / 2,
                boundingBox.getHeight() / 2,
                boundingBox.getWidthZ() / 2
        );
    }

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
        return getClosestSurfaceBlock(location, maxHeightDifference, keepXZ, UtilBlock::solid);
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
     * @param filter              The filter to apply to the blocks
     * @return The closest surface block, or {@link Optional#empty()} if none was found
     */
    public static Optional<Location> getClosestSurfaceBlock(final Location location, final double maxHeightDifference, final boolean keepXZ, final Predicate<Block> filter) {
        Preconditions.checkState(maxHeightDifference > 0, "Max height difference must be greater than 0");
        final int y = location.getBlockY();
        Block block = location.getBlock();
        while (filter.test(block)) { // Replacing the solid block with the one above it until we hit a non-solid block
            block = block.getRelative(BlockFace.UP);
            if (Math.abs(y - block.getY()) > maxHeightDifference) {
                return Optional.empty(); // If the block is too high, there wasn't a surface block
            }
        }
        // At this point, we have the highest non-solid block closest to the block. This could mean we already looped to the first non-solid
        // or we never looped at all because the location was already non-solid, which means we need to loop down to find the first solid
        while (!filter.test(block.getRelative(BlockFace.DOWN))) {
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

    /**
     *
     * @param initialLocation the location that any relative coordinates refers to
     * @param coordinates A list of a 3 strings, with x, y, z coordinates  Accepts `~` notation in the front. Acceptable inputs: ["0", "0", "0"], gives the location 0, 0, 0 and ["~", "~1", "~0"], which gives the location initialLocation.x, initalLocation.y + 1, initialLocation.z.
     * @return the Location calculated
     */
    public static Location getTeleportLocation (Location initialLocation, String[] coordinates) {
        double x = 0, y = 0, z = 0;
        Location location = initialLocation.getWorld().getSpawnLocation();
        if (coordinates.length == 3) {
            if (coordinates[0].startsWith("~")) {
                coordinates[0] = coordinates[0].substring(1);
                x = initialLocation.getX();
            }
            try {
                x += Double.parseDouble(coordinates[0]);
            } catch (NumberFormatException e) {
                x += 0;
            }
            if (coordinates[1].startsWith("~")) {
                coordinates[1] = coordinates[1].substring(1);
                y = initialLocation.getY();
            }
            try {
                y += Double.parseDouble(coordinates[1]);
            } catch (NumberFormatException e) {
                y += 0;
            }

            if (coordinates[2].startsWith("~")) {
                coordinates[2] = coordinates[2].substring(1);
                z = initialLocation.getZ();
            }
            try {
                z += Double.parseDouble(coordinates[2]);
            } catch (NumberFormatException e) {
                z += 0;
            }
            location.set(x, y, z);
        }
        return location;
    }

}
