package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
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

 /**
 * Utility class for handling various location-based operations and manipulations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilLocation {

    /**
     * The default field of view (FOV) value used within the class. This value is represented in degrees
     * and determines the viewing angle. It is commonly used in calculations related to field of vision
     * for entities or players in the context of game mechanics.
     */
    public static final float DEFAULT_FOV = 73f;

    /**
     * Determines whether a block and a bounding box would collide based on the block's passability
     * and the collision between the bounding box and the block.
     *
     * @param block The block to check for potential collision. Must not be null.
     * @param boundingBox The bounding box to check for potential collision. Must not be null.
     * @return {@code true} if the block is not passable and the bounding box collides with it;
     *         {@code false} otherwise.
     */
    private static boolean wouldCollide(Block block, BoundingBox boundingBox) {
        return !block.isPassable() && UtilBlock.doesBoundingBoxCollide(boundingBox, block);
    }

    /**
     * Teleports the given entity forward in the direction it is facing by the specified distance.
     * The teleportation may involve collision checks, world border validation, and optional post-teleport
     * handling.
     *
     * @param entity the {@link LivingEntity} to be teleported. Must not be null.
     * @param teleportDistance the distance (in blocks) to teleport the entity forward.
     * @param fallDamage whether the entity should take fall damage as a result of the teleport.
     * @param then an optional {@link Consumer} that receives a boolean indicating whether the teleportation
     *             was successful. Can be null.
     */
    public static void teleportForward(final @NotNull LivingEntity entity, double teleportDistance, boolean fallDamage, @Nullable Consumer<Boolean> then) {
        teleportToward(entity, entity.getEyeLocation().getDirection(), teleportDistance, fallDamage, then);
    }

    /**
     * Teleports an entity toward a specified direction within a given distance, respecting world boundaries
     * and collision rules. The teleportation process checks for obstacles, suffocation, and line of sight,
     * and adjusts the final location to avoid phasing or getting stuck in blocks.
     *
     * @param entity The entity to be teleported. Must not be null.
     * @param direction The direction vector determining the teleportation direction. Must not be null.
     * @param teleportDistance The maximum distance the entity can be teleported.
     * @param fallDamage Whether the entity should take fall damage upon teleportation.
     * @param then An optional callback to handle the result of the teleportation operation (true if successful, false otherwise).
     */
    public static void teleportToward(final @NotNull LivingEntity entity, final @NotNull Vector direction, double teleportDistance, boolean fallDamage, @Nullable Consumer<Boolean> then) {
        // Iterate from their location to their destination
        // Modify the base location by the direction they are facing
        direction.normalize();
        Location teleportLocation = entity.getLocation();
        WorldBorder worldBorder = teleportLocation.getWorld().getWorldBorder();

        final int iterations = (int) Math.ceil(teleportDistance / 0.2f);
        for (int i = 1; i <= iterations; i++) {
            // Extend their location by the direction they are facing by 0.2 blocks per iteration
            final Vector increment = direction.clone().multiply(0.2 * i);
            final Location newLocation = entity.getLocation().add(increment);

            if (!worldBorder.isInside(newLocation)) {
                // prevent teleport at all
                if (then != null) {
                    then.accept(Boolean.FALSE);
                }
                return;
            }

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

    /**
     * Calculates and returns a set of all corner locations that define the given bounding box
     * within the specified world. The bounding box is represented by its minimum and maximum
     * coordinates along the X, Y, and Z axes.
     *
     * @param world the world in which the bounding box is located
     * @param boundingBox the bounding box object providing the boundaries for the calculation
     * @return a set of {@code Location} objects representing the eight corner points of the bounding box
     */
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

    /**
     * Draws the bounding box by connecting its corners using lines.
     *
     * @param box the bounding box that defines the dimensions for rendering
     */
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

    /**
     * Draws a visual line with particles between two locations.
     * The line is visualized using red particles and is visible to nearby players.
     *
     * @param start the starting location of the line
     * @param end the ending location of the line
     */
    public static void drawBetween(Location start, Location end) {
        final VectorLine line = VectorLine.withStepSize(start, end, 0.15);
        for (Location location : line.toLocations()) {
            Particle.DUST.builder().location(location).color(Color.RED).receivers(50).spawn();
        }
    }

    /**
     * Calculates the set of points representing the circumference of a circle.
     *
     * @param center the center location of the circle
     * @param radius the radius of the circle (must be greater than 0)
     * @param points the number of points to generate on the circumference
     * @return a list of locations evenly distributed along the circumference
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

    /**
     * Generates a list of {@link Location} points that form a spherical shape based on the specified
     * center location, radius, and number of points.
     *
     * @param location the center {@link Location} of the sphere
     * @param radius the radius of the sphere, must be greater than 0
     * @param points the number of points used to construct the sphere
     * @return a {@link List} of {@link Location} instances representing the sphere
     * @throws IllegalStateException if the radius is not greater than 0
     */
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
     * Checks if a given location is in front of a specified living entity within a default field of view.
     *
     * @param entity the living entity whose field of view is being checked
     * @param other the location being checked for whether it is in front of the entity
     * @return true if the location is in front of the entity within the default field of view, false otherwise
     */
    public static boolean isInFront(final LivingEntity entity, final Location other) {
        return isInFront(entity, other, DEFAULT_FOV);
    }

    /**
     * Checks if a given location is in front of a living entity within a specified angle.
     *
     * @param entity the living entity whose viewpoint will be considered
     * @param other the location to check if it's in front of the entity
     * @param angle the maximum angle in degrees within which the location is considered in front
     * @return true if the location is within the specified angle in front of the entity, false otherwise
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
     * Adjusts the given location out of any intersecting blocks to prevent collisions.
     * The method modifies and returns a new location that is repositioned outside of any obstructing block.
     *
     * @param boundingBoxFloor the initial location representing the floor of the bounding box. This is used as
     *        the base location for determining collisions, and is modified during the process.
     * @param boundingBox the bounding box representing the area to check for collisions. It is used to detect
     *        overlaps with blocks in the world.
     * @return a modified location, shifted out of colliding blocks, ensuring the bounding box does not intersect
     *         with impassable or obstructive blocks in the world.
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
     * Copies the dimensions and center of the specified bounding box to a new location.
     * This is done by adjusting the location to center the bounding box and scaling it
     * proportionally based on its dimensions.
     *
     * @param boundingBox the bounding box whose dimensions and size are to be copied
     * @param boundingBoxFloorCenter the target location representing the new center for the bounding box's floor
     * @return a new bounding box centered at the specified location, with dimensions identical to the input bounding box
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
     * Calculates a new {@link Location} at a fixed angle and distance from a reference location.
     * The angle is measured in degrees clockwise from the north direction of the reference location.
     * The distance is specified as the radius.
     *
     * @param reference the reference {@link Location} from which the calculation is based
     * @param radius the distance to the new location, must be greater than zero
     * @param degree the angle in degrees clockwise from the north direction of the reference location
     * @return a new {@link Location} object representing the calculated position
     */
    public static Location fromFixedAngleDistance(final Location reference, final double radius, final double degree) {
        final Location north = reference.clone().setDirection(BlockFace.NORTH.getDirection());
        return fromAngleDistance(north, radius, degree);
    }

    /**
     * Calculates a new Location based on a reference Location, a radius, and an angle.
     * The resulting Location is computed by rotating a vector around the Y-axis by
     * the specified angle and adding it to the reference location.
     *
     * @param reference the reference Location used as the base point
     * @param radius the radius or distance from the reference Location; must be greater than 0
     * @param degree the angle in degrees to rotate around the Y-axis
     * @return the new Location calculated based on the input parameters
     * @throws IllegalArgumentException if the radius is not greater than 0
     */
    public static Location fromAngleDistance(final Location reference, final double radius, final double degree) {
        Preconditions.checkArgument(radius > 0, "Radius must be greater than 0");
        final Vector direction = reference.getDirection(); // unit-vector in the direction of the reference location
        direction.multiply(radius); // multiply by the radius to get a full-length vector in the direction of the reference
        direction.rotateAroundY(Math.toRadians(degree)); // rotate the vector around the y-axis by the specified angle
        return reference.clone().add(direction);
    }

    /**
     * Retrieves a list of blocks that represent the surface of a box-shaped area
     * centered around a given location, within a specified radius and height.
     *
     * @param center the central location of the box
     * @param radius the radius extending outward from the center along the x and z axes (must be greater than 0)
     * @param height the maximum height above and below the center to search for surface blocks
     * @return a list of blocks that constitute the surface of the box within the specified area
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
     * Retrieves the closest surface block near a given location within the maximum world height.
     *
     * @param location the initial {@link Location} from which to start the search for the surface block
     * @param keepXZ whether to fix the X and Z coordinates, searching only vertically along the Y-axis
     * @return an {@link Optional} containing the nearest {@link Block} on the surface if found, or an empty {@link Optional} if not found
     */
    public static Optional<Block> getClosestSurfaceBlock(final Location location, final boolean keepXZ) {
        return getClosestSurfaceBlock(location, location.getWorld().getMaxHeight(), keepXZ).map(Location::getBlock);
    }

    /**
     * Finds the closest surface block location relative to the provided location based on the height difference
     * and optionally maintains the X and Z coordinates of the original location.
     *
     * @param location The initial location from which to begin the search.
     * @param maxHeightDifference The maximum allowable height difference for the surface block search.
     *                             If exceeded during the search, the method returns an empty result.
     * @param keepXZ A boolean indicating whether the X and Z coordinates of the resulting location
     *               should match the input location's X and Z coordinates.
     * @return An {@link Optional} containing the closest surface block's location if found,
     *         or an empty {@link Optional} if no suitable surface block is within the height constraints.
     */
    public static Optional<Location> getClosestSurfaceBlock(final Location location, final double maxHeightDifference, final boolean keepXZ) {
        return getClosestSurfaceBlock(location, maxHeightDifference, keepXZ, UtilBlock::solid);
    }

    /**
     * Finds the closest surface block relative to the given location by traversing upwards and downwards
     * until a valid surface block is identified based on the specified conditions.
     *
     * @param location the starting location from which the search begins
     * @param maxHeightDifference the maximum vertical distance allowed between the starting location and
     *                            the potential surface block
     * @param keepXZ whether to maintain the X and Z coordinates of the original location in the result
     * @param filter a predicate used to determine whether a block is considered non-solid (or passable)
     * @return an Optional containing the location of the closest surface block if one is found within the
     *         height restrictions; otherwise, returns an empty Optional if no valid surface block can be located
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
     * Calculates a new teleportation location based on the initial location and the provided coordinates.
     * The coordinates can be absolute or relative, using the `~` symbol for relative values.
     *
     * @param initialLocation the reference location from which calculations are based
     * @param coordinates a string array of size 3 (x, y, z), where each value can be an absolute number
     *                    or a relative offset prefixed with `~`
     * @return the calculated Location object based on the provided coordinates. If invalid coordinates
     *         are provided, the world spawn location is used as a fallback.
     */
    public static Location getTeleportLocation(Location initialLocation, String[] coordinates) {
        double x = 0, y = 0, z = 0;
        Location location = initialLocation.getWorld().getSpawnLocation();
        if (coordinates.length >= 3) {
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

    /**
     * Finds all entities within a certain radius of the specified location.
     *
     * @param location the central location from which to search for nearby entities
     * @param radius the radius around the location to search for entities
     * @return a list of entities within the specified radius of the given location
     */
    public static List<Entity> getNearbyEntities(Location location, double radius) {
        World world = location.getWorld();
        if (world == null) return new ArrayList<>(); // Safety check

        List<Entity> entities = new ArrayList<>();
        double radiusSquared = radius * radius;

        // Convert location to chunk coordinates
        int centerX = location.getBlockX() >> 4; // Divide by 16
        int centerZ = location.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil(radius / 16.0); // Chunks to check in each direction

        // Iterate over chunks within the radius
        for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) {
            for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
                if (world.isChunkLoaded(x, z)) {
                    Chunk chunk = world.getChunkAt(x, z);
                    for (Entity entity : chunk.getEntities()) {
                        // Check if entity is within the bounding box first (faster than distance)
                        Location entityLoc = entity.getLocation();
                        double dx = Math.abs(entityLoc.getX() - location.getX());
                        double dy = Math.abs(entityLoc.getY() - location.getY());
                        double dz = Math.abs(entityLoc.getZ() - location.getZ());

                        if (dx <= radius && dy <= radius && dz <= radius) {
                            // Refine with spherical check
                            if (entityLoc.distanceSquared(location) <= radiusSquared) {
                                entities.add(entity);
                            }
                        }
                    }
                }
            }
        }
        return entities;
    }

    /**
     * Retrieves a list of all living entities within a specified radius around the given location.
     * Non-living entities are filtered out from the result.
     *
     * @param location the central location around which to find nearby living entities
     * @param radius the radius around the location to search for living entities
     * @return a list of {@link LivingEntity} objects that are within the specified radius of the location
     */
    public static List<LivingEntity> getNearbyLivingEntities(Location location, double radius) {
        return getNearbyEntities(location, radius).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the midpoint between two given locations.
     *
     * @param min the first location, representing one corner of the bounding area
     * @param max the second location, representing the opposite corner of the bounding area
     * @return a {@code Location} object representing the midpoint between the provided locations
     */
    public static Location getMidpoint(Location min, Location max) {
        return new Location(min.getWorld(),
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
    }
}
