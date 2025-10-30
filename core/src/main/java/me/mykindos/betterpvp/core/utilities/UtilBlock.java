package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Utility class providing various methods for working with blocks, entities, and materials
 * in a Minecraft-like environment. Contains static methods to perform checks, retrieve
 * blocks, manipulate block states, and more.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilBlock {

    /**
     * Scans a cubic region centered at the specified location, with the given radii, to find a block that matches the provided predicate.
     *
     * @param center the center location for the scan, must not be null
     * @param radiusX the radius along the X-axis to scan, must be greater than 0
     * @param radiusY the radius along the Y-axis to scan, must be greater than 0
     * @param radiusZ the radius along the Z-axis to scan, must be greater than 0
     * @param predicate the condition to test blocks against within the specified region
     * @return an {@code Optional} containing the first block that satisfies the predicate, or {@code Optional.empty()} if no matching block is found
     */
    public static Optional<Block> scanCube(@NotNull final Location center, int radiusX, int radiusY, int radiusZ, Predicate<Block> predicate) {
        Preconditions.checkArgument(radiusX > 0, "Radius must be greater than 0");
        Preconditions.checkArgument(radiusY > 0, "Radius must be greater than 0");
        Preconditions.checkArgument(radiusZ > 0, "Radius must be greater than 0");
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    final Location location = center.clone().add(x, y, z);
                    if (predicate.test(location.getBlock())) {
                        return Optional.of(location.getBlock());
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all bounding boxes associated with the given block. The bounding boxes
     * represent the shape of the block's collision area, adjusted for the block's location in the world.
     *
     * @param block the block whose bounding boxes are being retrieved, must not be null
     * @return a collection of bounding boxes representing the block's collision shape
     */
    public static Collection<BoundingBox> getBoundingBoxes(final Block block) {
        return block.getCollisionShape().getBoundingBoxes().stream().map(boundingBox -> {
            final Vector min = boundingBox.getMin().add(block.getLocation().toVector());
            final Vector max = boundingBox.getMax().add(block.getLocation().toVector());
            return BoundingBox.of(min, max);
        }).toList();
    }

    /**
     * Determines if a given bounding box collides with any of the bounding boxes of a specified block.
     *
     * @param boundingBox The bounding box to check for collisions, must not be null.
     * @param block The block whose bounding boxes will be checked for collision with the provided bounding box.
     * @return {@code true} if the provided bounding box collides with any bounding box of the block, otherwise {@code false}.
     */
    public static boolean doesBoundingBoxCollide(final BoundingBox boundingBox, final Block block) {
        final Collection<BoundingBox> boundingBoxes = getBoundingBoxes(block);
        for (final BoundingBox box : boundingBoxes) {
            if (box.overlaps(boundingBox)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the specified player is currently standing in lava.
     *
     * @param player the player to check, must not be null
     * @return {@code true} if the player is in lava, otherwise {@code false}
     */
    public static boolean isInLava(Player player) {

        return player.getLocation().getBlock().getType() == Material.LAVA;
    }

    /**
     * Checks if the specified location is within the "tutorial" world.
     *
     * @param loc the location to check, must not be null
     * @return {@code true} if the location is in the "tutorial" world, otherwise {@code false}
     */
    public static boolean isTutorial(Location loc) {

        return loc.getWorld().getName().equals("tutorial");
    }


    /**
     * Determines if the specified livingEntity is currently in water.
     *
     * This method checks if the livingEntity is submerged in water, swimming,
     * or standing in a block that is waterlogged.
     *
     * @param livingEntity the livingEntity to check, must not be null
     * @return {@code true} if the livingEntity is in water, swimming, or in a waterlogged block; {@code false} otherwise
     */
    public static boolean isInWater(LivingEntity livingEntity) {
        Block block = livingEntity.getLocation().getBlock();

        return isWater(block) || livingEntity.isSwimming() || (block.getBlockData() instanceof Waterlogged wl && wl.isWaterlogged());
    }

    /**
     * Determines if the specified block is considered water or closely related materials.
     *
     * @param block the block to check, must not be null
     * @return {@code true} if the block is of type WATER, BUBBLE_COLUMN, SEAGRASS, TALL_SEAGRASS, KELP, or KELP_PLANT; otherwise {@code false}
     */
    public static boolean isWater(Block block) {
        return block.getType() == Material.WATER
                || block.getType() == Material.BUBBLE_COLUMN
                || block.getType() == Material.SEAGRASS
                || block.getType() == Material.TALL_SEAGRASS
                || block.getType() == Material.KELP
                || block.getType() == Material.KELP_PLANT;
    }

    /**
     * Retrieves the target block that the player is looking at within a specified range.
     * The method iterates through the blocks in the player's line of sight and returns
     * the first non-air block it encounters.
     *
     * @param p the player whose line of sight is being traced, must not be null
     * @param range the maximum distance to search along the player's line of sight
     * @return the first non-air block within the specified range in the player's line of sight,
     *         or the last block in the line of sight if no solid block is found
     */
    public static final Block getTarget(Player p, int range) {
        BlockIterator iter = new BlockIterator(p, range);
        Block lastBlock = iter.next();
        while (iter.hasNext()) {
            lastBlock = iter.next();
            if (lastBlock.getType() == Material.AIR) {
                continue;
            }
            break;
        }
        return lastBlock;
    }

    /**
     * Determines if the given material is a type of non-stripped log.
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material is a non-stripped log, {@code false} otherwise
     */
    public static boolean isNonStrippedLog(Material material) {
        final Material[] validLogTypes = new Material[]{
                Material.OAK_LOG,
                Material.ACACIA_LOG,
                Material.BIRCH_LOG,
                Material.DARK_OAK_LOG,
                Material.JUNGLE_LOG,
                Material.SPRUCE_LOG,
                Material.CHERRY_LOG,
                Material.MANGROVE_LOG
        };

        return Arrays.asList(validLogTypes).contains(material);
    }

    /**
     * Determines if the given material represents a type of log in its naming convention.
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material's name ends with "_LOG", otherwise {@code false}
     */
    public static boolean isLog(Material material) {
        return material.toString().endsWith("_LOG");
    }


    /**
     * Determines whether the given material corresponds to a raw ore type.
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material is considered a raw ore, otherwise {@code false}
     */
    public static boolean isRawOre(Material material) {
        final Material[] validOreTypes = new Material[]{
                Material.STONE,
                Material.IRON_ORE,
                Material.DEEPSLATE_IRON_ORE,
                Material.GOLD_ORE,
                Material.DEEPSLATE_GOLD_ORE,
        };

        return Arrays.asList(validOreTypes).contains(material);
    }

    /**
     * Checks if the specified material represents an ore.
     *
     * An ore is defined as any material whose name ends with "_ORE"
     * or is specifically the material "GILDED_BLACKSTONE".
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material is an ore, otherwise {@code false}
     */
    public static boolean isOre(Material material) {
        return material.name().endsWith("_ORE") || material == Material.GILDED_BLACKSTONE;
    }

    /**
     * Checks if the given entity is currently standing on a block of the specified material.
     *
     * @param ent the entity to check
     * @param material the material to compare with the block the entity is standing on
     * @return true if the entity is standing on a block of the specified material, false otherwise
     */
    public static boolean isStandingOn(Entity ent, Material material) {
        return isStandingOn(ent, material.name());
    }

    /**
     * Checks if the given entity is standing on a specific material (block type).
     * For players, the method utilizes the bounding box of the entity to perform a precise
     * grid-based check beneath their feet. For other entities, it only checks if the entity
     * is on the ground.
     *
     * @param ent The entity to check. This can be a player or any other entity.
     * @param material The name of the material (block type) to check for, case-insensitive.
     * @return true if the entity is standing on the specified material, or for other entities
     *         if they are on the ground; false otherwise.
     */
    public static boolean isStandingOn(Entity ent, String material) {
        if (!(ent instanceof Player player)) {
            return ent.isOnGround();
        }

        final World world = player.getWorld();
        final BoundingBox reference = player.getBoundingBox();

        // Create a thin slice below the player's feet to check for collision
        final BoundingBox collisionBox = reference.clone().shift(0, -0.05, 0);
        collisionBox.expand(0, 0.05, 0); // Make it a bit thicker downward so we catch slight variations

        // Define how many points to check along each axis
        final int checkPointsX = 3; // Check left, center, right
        final int checkPointsZ = 3; // Check front, center, back

        // Calculate step sizes for X and Z axes
        final double stepX = (reference.getMaxX() - reference.getMinX()) / (checkPointsX - 1);
        final double stepZ = (reference.getMaxZ() - reference.getMinZ()) / (checkPointsZ - 1);

        // Check a grid of points under the player's hitbox for more accurate detection
        for (int ix = 0; ix < checkPointsX; ix++) {
            for (int iz = 0; iz < checkPointsZ; iz++) {
                double x = reference.getMinX() + (stepX * ix);
                double z = reference.getMinZ() + (stepZ * iz);

                Block block = new Location(world, x, reference.getMinY() - 0.05, z).getBlock();
                if (block.getType().name().toLowerCase().contains(material.toLowerCase()) &&
                        doesBoundingBoxCollide(collisionBox, block)) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Determines if the entity associated with the provided UUID is grounded.
     *
     * @param uuid the UUID of the entity to check, must not be null
     * @return {@code true} if the entity is grounded, otherwise {@code false}
     * @throws RuntimeException if the entity with the specified UUID does not exist
     */
    public static boolean isGrounded(UUID uuid) {
        Entity entity = Bukkit.getEntity(uuid);
        if (entity != null) {
            return isGrounded(entity);
        }

        throw new RuntimeException("Entity with UUID " + uuid + " does not exist");
    }

    /**
     * Determines if the specified entity is considered "grounded," meaning it is
     * in contact with the ground or a block. This method delegates to {@link #isGrounded(Entity, int)}
     * with the default number of blocks to check set to 1.
     *
     * @param ent the entity to check, must not be null
     * @return {@code true} if the entity is considered grounded, {@code false} otherwise
     */
    public static boolean isGrounded(Entity ent) {
        return isGrounded(ent, 1);
    }

    /**
     * Determines whether the specified entity is considered grounded within a certain number of blocks below it.
     * This takes into account whether the entity is a player or another type of entity.
     *
     * @param ent the entity to check, must not be null
     * @param numBlocks the number of blocks below the entity to check for ground support
     * @return {@code true} if the entity is considered grounded, otherwise {@code false}
     */
    public static boolean isGrounded(Entity ent, int numBlocks) {
        if (!(ent instanceof Player player)) {
            return ent.isOnGround();
        }

        final World world = player.getWorld();
        final BoundingBox reference = player.getBoundingBox();

        for (int i = 0; i < numBlocks; i++) {
            final BoundingBox collisionBox = reference.clone().shift(0, -0.1 - i, 0);
            Block block = new Location(world, reference.getMinX(), reference.getMinY() - 0.1 - i, reference.getMinZ()).getBlock();
            if (solid(block) && doesBoundingBoxCollide(collisionBox, block)) {
                return true;
            }

            block = new Location(world, reference.getMinX(), reference.getMinY() - 0.1 - i, reference.getMaxZ()).getBlock();
            if (solid(block) && doesBoundingBoxCollide(collisionBox, block)) {
                return true;
            }

            block = new Location(world, reference.getMaxX(), reference.getMinY() - 0.1 - i, reference.getMinZ()).getBlock();
            if (solid(block) && doesBoundingBoxCollide(collisionBox, block)) {
                return true;
            }


        }

        final BoundingBox collisionBox = reference.clone().shift(0, -0.1, 0);
        Block block = new Location(world, reference.getMaxX(), reference.getMinY() - 0.1, reference.getMaxZ()).getBlock();
        return solid(block) && doesBoundingBoxCollide(collisionBox, block);

    }

    /**
     * Retrieves the block located directly beneath the given location by lowering its Y-coordinate by 1.
     *
     * @param location the location from which to find the block underneath, must not be null
     * @return the block located directly below the specified location
     */
    public static Block getBlockUnder(Location location) {
        location.setY(location.getY() - 1);

        return location.getBlock();
    }


    /**
     * Sets the type of block at the specified location to the given material.
     *
     * @param loc the location where the block type should be set, must not be null
     * @param m the material to set the block to, must not be null
     */
    public static void setBlock(Location loc, Material m) {
        loc.getWorld().getBlockAt(loc).setType(m);
    }


    /**
     * Retrieves all surrounding blocks of the specified block. If diagonals are included,
     * all adjacent blocks in a 3x3x3 cube around the block are returned. If diagonals
     * are excluded, only the directly adjacent blocks (up, down, north, south, east, west) are returned.
     *
     * @param block the base block for which the surrounding blocks are to be retrieved
     * @param diagonals a boolean indicating whether diagonal blocks should be included
     * @return an ArrayList containing the surrounding blocks
     */
    public static ArrayList<Block> getSurrounding(Block block, boolean diagonals) {
        ArrayList<Block> blocks = new ArrayList<>();
        if (diagonals) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if ((x != 0) || (y != 0) || (z != 0)) {
                            blocks.add(block.getRelative(x, y, z));
                        }
                    }
                }
            }
        } else {
            blocks.add(block.getRelative(BlockFace.UP));
            blocks.add(block.getRelative(BlockFace.DOWN));
            blocks.add(block.getRelative(BlockFace.NORTH));
            blocks.add(block.getRelative(BlockFace.SOUTH));
            blocks.add(block.getRelative(BlockFace.EAST));
            blocks.add(block.getRelative(BlockFace.WEST));
        }
        return blocks;
    }


    /**
     * Retrieves all blocks within a specified radius around a given location, considering the distance to each block.
     * The method utilizes a maximum height limit for the search to constrain the vertical range.
     *
     * @param loc the central location from which the radius is calculated, must not be null
     * @param dR the radius within which blocks are searched, must be greater than 0
     * @return a map of blocks within the radius and their corresponding normalized proximity values,
     *         where closer blocks have higher proximity values
     */
    public static HashMap<Block, Double> getInRadius(Location loc, double dR) {
        return getInRadius(loc, dR, 999.0D);
    }

    /**
     * Gets all blocks within a specified radius around a given location, considering a height limit.
     * Each block is mapped to a weight value based on its distance from the center location.
     * The farther the block is from the center, the lower its weight.
     *
     * @param loc the central location from which to calculate the radius, must not be null
     * @param dR the radius within which blocks will be considered
     * @param heightLimit the maximum absolute height above or below the central location to include blocks
     * @return a HashMap where the keys are the blocks within the radius, and the values are their respective weights
     */
    public static HashMap<Block, Double> getInRadius(Location loc, double dR, double heightLimit) {
        HashMap<Block, Double> blockList = new HashMap<Block, Double>();
        int iR = (int) dR + 1;

        for (int x = -iR; x <= iR; x++) {
            for (int z = -iR; z <= iR; z++) {
                for (int y = -iR; y <= iR; y++) {

                    if (Math.abs(y) <= heightLimit) {
                        Block curBlock = loc.getWorld().getBlockAt((int) (loc.getX() + x), (int) (loc.getY() + y), (int) (loc.getZ() + z));

                        double offset = UtilMath.offset(loc, curBlock.getLocation().add(0.5D, 0.5D, 0.5D));

                        if (offset <= dR) {
                            blockList.put(curBlock, 1.0D - offset / dR);
                        }
                    }
                }
            }
        }
        return blockList;
    }

    /**
     * Retrieves all blocks within a specified radius around a given block and calculates the normalized distance
     * of each block from the center block as a value between 0.0 and 1.0.
     *
     * @param block the central block from which the radius is calculated, must not be null
     * @param dR the radius within which blocks will be included, must be greater than or equal to 0
     * @return a map containing blocks within the radius as keys and their normalized distances (1.0 - distance/radius) as values
     */
    public static HashMap<Block, Double> getInRadius(Block block, double dR) {
        HashMap<Block, Double> blockList = new HashMap<Block, Double>();
        int iR = (int) dR + 1;
        for (int x = -iR; x <= iR; x++) {
            for (int z = -iR; z <= iR; z++) {
                for (int y = -iR; y <= iR; y++) {
                    Block curBlock = block.getRelative(x, y, z);

                    double offset = UtilMath.offset(block.getLocation(), curBlock.getLocation());
                    if (offset <= dR) {
                        blockList.put(curBlock, 1.0D - offset / dR);
                    }
                }
            }
        }
        return blockList;
    }

    /**
     *
     */
    public static boolean abilityCheck(Block block) {
        if (block == null) {
            return true;
        }

        return !block.getType().toString().contains("_DOOR") && block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST
                && block.getType() != Material.ANVIL;
    }

    /**
     * Determines whether the specified material represents air or foliage.
     * This is done by checking if the material is non-solid.
     *
     * @param mat the material to evaluate, must not be null
     * @return {@code true} if the material is not solid, otherwise {@code false}
     */
    public static boolean airFoliage(Material mat) {
        return !mat.isSolid();
    }


    /**
     * Evaluates whether a block is considered "air foliage" by checking its material property.
     * A block is deemed "air foliage" if it has no solid material.
     *
     * @param block the block to check, can be null
     * @return true if the block is non-null and its material is not solid, false otherwise
     */
    public static boolean airFoliage(Block block) {
        if (block == null) {
            return false;
        }
        return airFoliage(block.getType());
    }

    /**
     * Determines if the specified material is solid.
     *
     * @param mat the material to check, must not be null
     * @return {@code true} if the material is solid, otherwise {@code false}
     */
    public static boolean solid(Material mat) {
        return mat.isSolid() || mat == Material.SNOW;
    }

    /**
     * Determines if the given block is solid. A block is considered solid if it is not null
     * and its material type is classified as solid.
     *
     * @param block the block to check, may be null
     * @return {@code true} if the block is solid, otherwise {@code false}
     */
    public static boolean solid(Block block) {
        if (block == null) {
            return false;
        }
        return solid(block.getType());
    }

    /**
     * Determines if the given block is usable.
     * A block is considered usable based on its type and whether it satisfies certain criteria.
     *
     * @param block the block to check, may be {@code null}
     * @return {@code true} if the block is considered usable, otherwise {@code false}
     */
    public static boolean usable(Block block) {
        if (block == null) {
            return false;
        }
        return usable(block.getType());
    }

    /**
     * Determines if the given material is usable, based on its properties or name pattern.
     *
     * A material is considered usable if it is interactable, or if its name contains certain patterns
     * (e.g., "STAIR", "FENCE", "WIRE"), or if it is a type of log.
     *
     * @param mat the material to check, must not be null
     * @return {@code true} if the material is usable, otherwise {@code false}
     */
    public static boolean usable(Material mat) {
        boolean interactable = mat.isInteractable();
        return interactable || mat.name().contains("STAIR") || mat.name().contains("FENCE") || mat.name().contains("WIRE")
                || UtilBlock.isLog(mat);
    }

    /**
     *
     */
    public static boolean isStickyBlock(Block block) {
        return isStickyBlock(block.getType());
    }

    /**
     * Determines if the provided material is a sticky block.
     * Sticky blocks are materials like SLIME_BLOCK and HONEY_BLOCK
     * that have sticky properties in the game.
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material is a sticky block, otherwise {@code false}
     */
    public static boolean isStickyBlock(Material material) {
        return material == Material.SLIME_BLOCK || material == Material.HONEY_BLOCK;
    }

    /**
     * Determines if the specified block is considered a redstone-related block.
     *
     * @param block the block to check, must not be null
     * @return {@code true} if the block is redstone-related, otherwise {@code false}
     */
    public static boolean isRedstone(Block block) {
        return isRedstone(block.getType());
    }

    /**
     * Determines if the given material is considered a redstone-related block.
     * This includes materials with block data that implement Powerable,
     * AnaloguePowerable, Openable, or Lightable interfaces, as well as
     * the specific REDSTONE_BLOCK material.
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material is associated with redstone
     *         functionality, otherwise {@code false}
     */
    public static boolean isRedstone(Material material) {
        BlockData blockData = material.createBlockData();
        return blockData instanceof Powerable || blockData instanceof AnaloguePowerable
                || blockData instanceof Openable || blockData instanceof Lightable || material == Material.REDSTONE_BLOCK;
    }
    /**
     * Determines if the specified entity is currently in a liquid. Liquid is
     * considered to be water, lava, or a bubble column.
     *
     * @param ent the entity to check, must not be null
     * @return {@code true} if the entity is in water, lava, or a bubble column;
     *         {@code false} otherwise
     */
    public static boolean isInLiquid(Entity ent) {
        if (ent instanceof Player player) {
            return isInWater(player) || isInLava(player) || ent.isInBubbleColumn();
        } else {
            return ent.isInWater() || ent.isInLava() || ent.isInBubbleColumn();
        }
    }

    /**
     * Determines if the specified block is considered a wall. A block is treated as a wall if it satisfies
     * certain conditions based on its surrounding blocks and its relative position.
     *
     * @param block the block to check, must not be null
     * @return {@code true} if the block is considered a wall, otherwise {@code false}
     */
    public static boolean isWall(Block block) {
        boolean relativeNorth = UtilBlock.airFoliage(block.getRelative(BlockFace.NORTH));
        boolean relativeSouth = UtilBlock.airFoliage(block.getRelative(BlockFace.SOUTH));
        boolean relativeEast = UtilBlock.airFoliage(block.getRelative(BlockFace.EAST));
        boolean relativeWest = UtilBlock.airFoliage(block.getRelative(BlockFace.WEST));
        return !UtilBlock.airFoliage(block) || !UtilBlock.airFoliage(block.getRelative(BlockFace.UP))
                || (!relativeWest && !relativeNorth)
                || (!relativeEast && !relativeNorth)
                || (!relativeEast && !relativeSouth)
                || (!relativeWest && !relativeSouth);
    }

    /**
     * Determines whether the given material is classified as cultivation.
     * Cultivation materials are typically those used for farming or agriculture in the game.
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material is considered a cultivation material, otherwise {@code false}
     */
    public static boolean isCultivation(Material material) {
        return material == Material.PUMPKIN_SEEDS || material == Material.MELON_SEEDS
                || material == Material.WHEAT_SEEDS
                || material == Material.SUGAR_CANE
                || material == Material.POTATO
                || material == Material.POTATOES
                || material == Material.CARROT
                || material == Material.CARROTS
                || material == Material.CACTUS
                || material == Material.MELON_STEM
                || material == Material.WHEAT
                || material == Material.PUMPKIN_STEM
                || material == Material.PUMPKIN
                || material == Material.MELON
                || material == Material.NETHER_WART_BLOCK
                || material == Material.NETHER_WART
                || material == Material.BEETROOT
                || material == Material.BEETROOTS
                || material == Material.SWEET_BERRY_BUSH

                || material == Material.SWEET_BERRIES;
    }


    /**
     * Determines if the specified material is categorized as a type of seed or planting material.
     *
     * @param material the material to check, must not be null
     * @return {@code true} if the material is a seed or planting material, otherwise {@code false}
     */
    public static boolean isSeed(Material material) {
        return material == Material.PUMPKIN_SEEDS || material == Material.MELON_SEEDS
                || material == Material.WHEAT_SEEDS || material == Material.SUGAR_CANE
                || material == Material.POTATO
                || material == Material.WHEAT
                || material == Material.CARROT
                || material == Material.CACTUS
                || material == Material.NETHER_WART_BLOCK
                || material == Material.NETHER_WART
                || material == Material.BEETROOT_SEEDS
                || material == Material.SWEET_BERRIES
                || material == Material.SWEET_BERRY_BUSH;
    }

    /**
     * Determines whether snow can be placed on the specified block.
     * The method checks various conditions such as whether the block is liquid,
     * the type of block it is, and its usability for placing snow.
     *
     * @param block the block to evaluate for snow placement, must not be null
     * @return {@code true} if snow can be placed on the block, otherwise {@code false}
     */
    public static boolean shouldPlaceSnowOn(Block block) {

        if (block.isLiquid()) return false;

        if (airFoliage(block)) return false;

        if (usable(block)) return false;

        if (isCultivation(block.getType()) || isSeed(block.getType())) return false;

        if (List.of(Material.DIRT_PATH, Material.FARMLAND, Material.RAIL).contains(block.getType())) return false;

        String name = block.getType().name();
        return !name.contains("STAIRS") && !name.contains("CAMPFIRE") && !name.contains("SLAB") && !name.contains("ICE")
                && !name.contains("WIRE") && !name.contains("FENCE");
    }

    /**
     * Computes a unique key for the given block based on its coordinates.
     * The key represents a combination of the block's X, Y, and Z coordinates.
     *
     * @param block the block for which the unique key is to be computed, must not be null
     * @return an integer representing the unique key for the specified block
     */
    public static long getBlockKey(Block block) {
        final long x = block.getX() % 16;
        final long y = block.getY();
        final long z = block.getZ() % 16;
        return y & 0xFFFF | (x & 0xFF) << 16 | (z & 0xFF) << 24;
    }


    /**
     * Breaks a block naturally as if a player mined it, dropping its appropriate items and playing
     * the corresponding sound effects. Handles effect-based logic, such as item reservation, if
     * applicable.
     *
     * @param block the block to be broken, must not be null
     * @param player the player responsible for breaking the block, must not be null
     * @param effectManager the manager for handling effects on the player, must not be null
     */
    public static void breakBlockNaturally(@NotNull Block block, @NotNull Player player, EffectManager effectManager) {
        final Location location = block.getLocation();
        final World world = location.getWorld();
        final List<Item> drops = block.getDrops(player.getInventory().getItemInMainHand(), player).stream()
                .map(itemStack -> world.dropItemNaturally(location, itemStack))
                .toList();
        final SoundGroup soundGroup = block.getBlockData().getSoundGroup();
        block.setType(Material.AIR, true);
        world.playSound(location, soundGroup.getBreakSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
        boolean isProtected = effectManager.hasEffect(player, EffectTypes.PROTECTION);
        if (isProtected) {
            drops.forEach(item -> UtilItem.reserveItem(item, player, 10.0));
        }
    }

    public static boolean isPressurePlate(Block block) {
        return block.getType().name().endsWith("_PLATE");
    }

}
