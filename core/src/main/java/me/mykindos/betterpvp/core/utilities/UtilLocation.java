package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilLocation {

    public static final float DEFAULT_FOV = 73f;

    private static boolean wouldCollide(Block block, BoundingBox boundingBox) {
        return !block.isPassable() && UtilBlock.doesBoundingBoxCollide(boundingBox, block);
    }

    public static void teleportForward(final @NotNull LivingEntity entity, double teleportDistance, boolean fallDamage, @Nullable Consumer<Boolean> then) {
        teleportToward(entity, entity.getEyeLocation().getDirection(), teleportDistance, fallDamage, then);
    }

    public static void teleportToward(final @NotNull LivingEntity entity, final @NotNull Vector direction, double teleportDistance, boolean fallDamage, @Nullable Consumer<Boolean> then) {
        // Iterate from their location to their destination
        // Modify the base location by the direction they are facing
        direction.normalize();
        Location teleportLocation = entity.getLocation();

        final int iterations = (int) Math.ceil(teleportDistance / 0.2f);
        for (int i = 1; i <= iterations; i++) {
            // Extend their location by the direction they are facing by 0.2 blocks per iteration
            final Vector increment = direction.clone().multiply(0.2 * i);
            final Location newLocation = entity.getLocation().add(increment);

            // Get the bounding box of the entity as if they were standing on the new location
            BoundingBox relativeBoundingBox = UtilLocation.copyAABBToLocation(entity.getBoundingBox(), newLocation);

            // Only cancel for collision if the block isn't passable AND we hit its collision shape
            final Location blockOnTop = newLocation.clone().add(0, 1.0, 0);
            if (wouldCollide(blockOnTop.getBlock(), relativeBoundingBox)) {
                break;
            }

            // We know they won't suffocate because we checked the block above them
            // Now check their feet and see if we can skip this block to allow for through-block flash
            Location newTeleportLocation = newLocation;
            if (wouldCollide(newLocation.getBlock(), relativeBoundingBox)) {
                // If the block at their feet is not passable, try to skip it IF
                // and ONLY IF there isn't a third block above forming a 1x1 gap
                // This allows for through-block flash
                if (!blockOnTop.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()) {
//                if (wouldCollide(blockOnTop.clone().add(0.0, 1.0, 0.0).getBlock(), relativeBoundingBox)) {
                    break;
                }

                // At this point, we can ATTEMPT to skip the block at their feet
                final Vector horizontalIncrement = increment.clone().setY(0);
                final Location frontLocation = entity.getLocation().add(horizontalIncrement);
                relativeBoundingBox = UtilLocation.copyAABBToLocation(entity.getBoundingBox(), frontLocation);
                if (wouldCollide(frontLocation.getBlock(), relativeBoundingBox)
                        || wouldCollide(frontLocation.clone().add(0, entity.getHeight() / 2, 0).getBlock(), relativeBoundingBox)
                        || wouldCollide(frontLocation.clone().add(0, entity.getHeight(), 0).getBlock(), relativeBoundingBox)) {
                    continue; // Cancel if that block we're skipping to is not passable
                }

                newTeleportLocation = frontLocation;
            }

            final Location headBlock = newTeleportLocation.clone().add(0.0, relativeBoundingBox.getHeight(), 0.0);
            if (wouldCollide(headBlock.getBlock(), relativeBoundingBox)) {
                break; // Stop raying if we hit a block above their head
            }

            if (!entity.hasLineOfSight(newTeleportLocation) || !entity.hasLineOfSight(headBlock)) {
                break; // Stop raying if we don't have line of sight
            }

            teleportLocation = newTeleportLocation;
        }

        // Adjust pitch and yaw to match the direction they are facing
        teleportLocation.setPitch(entity.getLocation().getPitch());
        teleportLocation.setYaw(entity.getLocation().getYaw());

        // Shift them out of the location to avoid PHASING and SUFFOCATION
        entity.leaveVehicle();
        teleportLocation = UtilLocation.shiftOutOfBlocks(teleportLocation, entity.getBoundingBox());

        // Teleport
        // Asynchronously because, for some reason, spigot fires PlayerInteractEvent twice if the entity looks at a block
        // causing them to use the skill again after being teleported
        // teleportAsync somehow fixes that
        entity.teleportAsync(teleportLocation).thenAccept(result ->  {
            if (!fallDamage) {
                entity.setFallDistance(0);
            }

            if (then != null) {
                then.accept(result);
            }
        });
    }

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
            Particle.DUST.builder().location(location).color(Color.RED).receivers(50).spawn();
        }
    }

    /**
     * Get a specified number of points on the circumference of a circle with the given radius and center.
     *
     * @param center The center of the circle
     * @param radius The radius of the circle
     * @param points The number of points to get
     * @return A list of locations on the circumference of the circle
     */
    public static List<Location> getCircumference(final Location center, final double radius, final int points) {
        Preconditions.checkState(radius > 0, "Radius must be greater than 0");
        final List<Location> circle = new ArrayList<>();
        final int increment = 360 / points;
        for (int i = 0; i < 360; i += increment) {
            circle.add(fromFixedAngleDistance(center, radius, i));
        }
        return circle;
    }

    public static List<Location> getSphere(final Location location, final double radius, final int points) {
        Preconditions.checkState(radius > 0, "Radius must be greater than 0");
        final List<Location> sphere = new ArrayList<>();
        for (double i = 0; i <= Math.PI; i += Math.PI / points) {
            double currentRadius = Math.sin(i) * radius;
            double y = Math.cos(i) * radius;
            for (double a = 0; a < Math.PI * 2; a+= Math.PI / points) {
                double x = Math.cos(a) * currentRadius;
                double z = Math.sin(a) * currentRadius;
                location.add(x, y, z);
                sphere.add(location.clone());
                location.subtract(x, y, z);
            }
        }
        return sphere;
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
                    final Vector directionToMove = blockAABB.clone().subtract(collidingBox.getCenter()).normalize();
                    final BoundingBox intersection = collidingBox.clone().intersection(effectiveAABB);

                    double deltaX = intersection.getWidthX() < intersection.getWidthZ() ? intersection.getWidthX() : 0f;
                    double deltaZ = intersection.getWidthX() > intersection.getWidthZ() ? intersection.getWidthZ() : 0f;
                    if (intersection.getHeight() <= 0.5) {
                        directionToMove.setY(1); // slabs and elevated blocks
                    }

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
     * Get all surface blocks in a box within a certain radius of a {@link Location}.
     *
     * @param center The center location for the circle
     * @param radius The radius of the circle
     * @return A {@link Map} of blocks and their distance from the center
     * @see #getClosestSurfaceBlock(Location, boolean)
     */
    public static List<Block> getBoxSurfaceBlocks(final Location center, final double radius, final double height) {
        Preconditions.checkState(radius > 0, "Radius must be greater than 0");
        final List<Block> blocks = new ArrayList<>();

        // Loop through all blocks in the radius within the same y-level as the center and get the closest surface block
        for (double x = -radius; x <= radius; x++) {
            for (double z = -radius; z <= radius; z++) {
                // Block at the same height as the center
                final Location blockLocation = center.clone().add(x, 0, z);
                // If there is a surface block, add it to the list
                getClosestSurfaceBlock(blockLocation, height, false).map(Location::getBlock).ifPresent(blocks::add);
            }
        }
        return blocks;
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
    public static Location getTeleportLocation(Location initialLocation, String[] coordinates) {
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
