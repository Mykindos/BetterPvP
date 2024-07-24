package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilBlock {

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

    public static Collection<BoundingBox> getBoundingBoxes(final Block block) {
        return block.getCollisionShape().getBoundingBoxes().stream().map(boundingBox -> {
            final Vector min = boundingBox.getMin().add(block.getLocation().toVector());
            final Vector max = boundingBox.getMax().add(block.getLocation().toVector());
            return BoundingBox.of(min, max);
        }).toList();
    }

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
     * Check if a Player is in Lava
     *
     * @param p The Player
     * @return Returns true if the Player is currently standing in lava.
     */
    public static boolean isInLava(Player p) {

        return p.getLocation().getBlock().getType() == Material.LAVA;
    }

    /**
     * Check if a Location is in the tutorial world
     *
     * @param loc Location you wish to check
     * @return Returns true if the locations world is the Tutorial world
     */
    public static boolean isTutorial(Location loc) {

        return loc.getWorld().getName().equals("tutorial");
    }


    /**
     * Check if a Player is in water
     *
     * @param player The Player
     * @return Returns true if the Player is standing in water
     */
    public static boolean isInWater(Player player) {
        Block block = player.getLocation().getBlock();

        return isWater(block) || player.isSwimming() || (block.getBlockData() instanceof Waterlogged wl && wl.isWaterlogged());
    }

    public static boolean isWater(Block block) {
        return block.getType() == Material.WATER
                || block.getType() == Material.BUBBLE_COLUMN
                || block.getType() == Material.SEAGRASS
                || block.getType() == Material.TALL_SEAGRASS
                || block.getType() == Material.KELP
                || block.getType() == Material.KELP_PLANT;
    }

    /**
     * Get the Block a player is looking at
     *
     * @param p     The player
     * @param range Max distance the block can be from a player
     * @return The block the player is looking at
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
     * Check if a material is a (non-stripped) log type
     */
    public static boolean isNonStrippedLog(Material material) {
        final Material[] validLogTypes = new Material[] {
                Material.OAK_LOG,
                Material.ACACIA_LOG,
                Material.BIRCH_LOG,
                Material.DARK_OAK_LOG,
                Material.JUNGLE_LOG,
                Material.SPRUCE_LOG
        };

        return Arrays.asList(validLogTypes).contains(material);
    }

    /**
     * Check if a material is a log (stripped or non-stripped)
     */
    public static boolean isLog(Material material) {
        return material.toString().endsWith("_LOG");
    }

    public static boolean isStandingOn(Entity ent, Material material) {
        return isStandingOn(ent, material.name());
    }

    public static boolean isStandingOn(Entity ent, String material) {
        if (!(ent instanceof Player player)) {
            return ent.isOnGround();
        }

        final World world = player.getWorld();
        final BoundingBox reference = player.getBoundingBox();


        final BoundingBox collisionBox = reference.clone().shift(0, -0.1, 0);
        Block block = new Location(world, reference.getMinX(), reference.getMinY() - 0.1, reference.getMinZ()).getBlock();
        if (block.getType().name().toLowerCase().contains(material.toLowerCase()) && doesBoundingBoxCollide(collisionBox, block)) {
            return true;
        }

        block = new Location(world, reference.getMinX(), reference.getMinY() - 0.1, reference.getMaxZ()).getBlock();
        if (block.getType().name().toLowerCase().contains(material.toLowerCase()) && doesBoundingBoxCollide(collisionBox, block)) {
            return true;
        }

        block = new Location(world, reference.getMaxX(), reference.getMinY() - 0.1, reference.getMinZ()).getBlock();
        if (block.getType().name().toLowerCase().contains(material.toLowerCase()) && doesBoundingBoxCollide(collisionBox, block)) {
            return true;
        }

        block = new Location(world, reference.getMaxX(), reference.getMinY() - 0.1, reference.getMaxZ()).getBlock();
        if (block.getType().name().toLowerCase().contains(material.toLowerCase()) && doesBoundingBoxCollide(collisionBox, block)) {
            return true;
        }

        return false;

    }

    /**
     * Check if an Entity is on the ground
     *
     * @param ent The Entity
     * @return Returns true if the entity is on the ground
     */
    public static boolean isGrounded(Entity ent) {
        return isGrounded(ent, 1);
    }

    /**
     * Check if an Entity is on the ground
     *
     * @param ent       The Entity
     * @param numBlocks The number of blocks below the ent to check
     * @return Returns true if the entity is on the ground
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
     * Gets the block under the location provided
     *
     * @param location The location to check
     * @return The block under the location provided
     */
    public static Block getBlockUnder(Location location) {
        location.setY(location.getY() - 1);

        return location.getBlock();
    }


    /**
     * Set the block type at a specific location
     *
     * @param loc Location to change
     * @param m   Material of the new block
     */
    public static void setBlock(Location loc, Material m) {
        loc.getWorld().getBlockAt(loc).setType(m);
    }


    /**
     * Returns a list of blocks surrounding the block provided
     *
     * @param block     The block to check
     * @param diagonals Whether or not to check the blocks diagnol of block
     * @return An ArrayList of blocks that are surrounding the block provided
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
     * Gets a Map of All blocks and their distance from the location provided
     *
     * @param loc The location to check
     * @param dR  The max radius to check for blocks
     * @return A HashMap of <Block, Double> containing all blocks and their distance from a location
     */
    public static HashMap<Block, Double> getInRadius(Location loc, double dR) {
        return getInRadius(loc, dR, 999.0D);
    }

    /**
     * Gets a Map of All blocks and their distance from the location provided
     *
     * @param loc         The location to check
     * @param dR          The max radius to check for blocks
     * @param heightLimit Max distance to check up and down
     * @return A HashMap of <Block, Double> containing all blocks and their distance from a location
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
     * Gets a Map of All blocks and their distance from the location provided
     *
     * @param block The location to check
     * @param dR    The max radius to check for blocks
     * @return A HashMap of <Block, Double> containing all blocks and their distance from a location
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
     * Check if a specific block is a chest, door, or other type of interactable block
     *
     * @param block Block to check
     * @return Returns true if an ability can be casted, False if block can be interacted with
     */
    public static boolean abilityCheck(Block block) {
        if (block == null) {
            return true;
        }

        return !block.getType().toString().contains("_DOOR") && block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST
                && block.getType() != Material.ANVIL;
    }

    /**
     * Check if block is a block entities can walk through (e.g. Long grass)
     *
     * @return Returns true if the block does not stop player movement
     */
    public static boolean airFoliage(Material mat) {
        return !mat.isSolid();
    }


    /**
     * Check if block is a block entities can walk through (e.g. Long grass)
     *
     * @param block Block to check
     * @return Returns true if the block does not stop player movement
     */
    public static boolean airFoliage(Block block) {
        if (block == null) {
            return false;
        }
        return airFoliage(block.getType());
    }

    /**
     * Check if a block is a solid block
     *
     * @return Returns true if the block is solid (e.g. Stone)
     */
    public static boolean solid(Material mat) {
        return mat.isSolid();
    }

    /**
     * Check if a block is a solid block
     *
     * @param block Block to check
     * @return Returns true if the block is solid (e.g. Stone)
     */
    public static boolean solid(Block block) {
        if (block == null) {
            return false;
        }
        return solid(block.getType());
    }

    /**
     * Check if a block is usable (can interact with)
     *
     * @param block Block ID to check
     * @return True if the block can be interacted with. (E.g. a chest or door)
     */
    public static boolean usable(Block block) {
        if (block == null) {
            return false;
        }
        return usable(block.getType());
    }

    /**
     * Check if a block is usable (can interact with)
     *
     * @return True if the block can be interacted with. (E.g. a chest or door)
     */
    public static boolean usable(Material mat) {
        boolean interactable = mat.isInteractable();
        return interactable || mat.name().contains("STAIR") || mat.name().contains("FENCE") || mat.name().contains("WIRE");
    }

    /**
     * @param block Block ID to check
     * @return True if it is a Slime or Honey block
     */
    public static boolean isStickyBlock(Block block) {
        return isStickyBlock(block.getType());
    }

    public static boolean isStickyBlock(Material material) {
        return material == Material.SLIME_BLOCK || material == Material.HONEY_BLOCK;
    }

    public static boolean isInLiquid(Entity ent) {
        if (ent instanceof Player player) {
            return isInWater(player) || isInLava(player) || ent.isInBubbleColumn();
        } else {
            return ent.isInWater() || ent.isInLava() || ent.isInBubbleColumn();
        }
    }

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
     * Get the persistent data container for a block
     *
     * @param block The block to get the container for
     * @return The persistent data container for the block
     */
    public static PersistentDataContainer getPersistentDataContainer(Block block) {
        final Chunk chunk = block.getChunk();
        final PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        // We have no data for this chunk, just make a new one
        if (!chunkPdc.has(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY)) {
            return chunkPdc.getAdapterContext().newPersistentDataContainer();
        }

        final PersistentDataContainer[] blockData = chunkPdc.get(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY);
        if (blockData == null) {
            throw new RuntimeException("Block PDC is null");
        }

        final int blockKey = getBlockKey(block);
        for (PersistentDataContainer uniqueData : blockData) {
            int key = Objects.requireNonNull(uniqueData.get(CoreNamespaceKeys.BLOCK_TAG_KEY, PersistentDataType.INTEGER));
            if (key == blockKey) {
                return uniqueData; // Return if one container has the same key
            }
        }

        // If none was found for this block, just make a new one
        return chunkPdc.getAdapterContext().newPersistentDataContainer();
    }

    /**
     * Set the persistent data container for a block
     *
     * @param block     The block to set the container for
     * @param container The container to set
     */
    public static void setPersistentDataContainer(Block block, PersistentDataContainer container) {
        // Before anything, make sure the key is set
        container.set(CoreNamespaceKeys.BLOCK_TAG_KEY, PersistentDataType.INTEGER, getBlockKey(block));

        final boolean remove = container.getKeys().size() == 1; // Remove if we only have the key
        final Chunk chunk = block.getChunk();
        final PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        // We have no data for this chunk, just make a new one
        if (!chunkPdc.has(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY)) {
            if (remove) {
                return; // Don't set if we have no data and we're removing
            }
            chunkPdc.set(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY, new PersistentDataContainer[]{container});
            return;
        }

        final PersistentDataContainer[] blockData = chunkPdc.get(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY);
        if (blockData == null) {
            throw new RuntimeException("Block PDC is null");
        }

        final int blockKey = getBlockKey(block);
        for (int i = 0; i < blockData.length; i++) {
            PersistentDataContainer uniqueData = blockData[i];
            int key = Objects.requireNonNull(uniqueData.get(CoreNamespaceKeys.BLOCK_TAG_KEY, PersistentDataType.INTEGER));
            if (key == blockKey) {
                if (remove) {
                    // If we're removing, remove the element from the array
                    final PersistentDataContainer[] newData = new PersistentDataContainer[blockData.length - 1];
                    System.arraycopy(blockData, 0, newData, 0, i);
                    System.arraycopy(blockData, i + 1, newData, i, blockData.length - i - 1);
                    chunkPdc.set(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY, newData);
                } else {
                    // Otherwise tne container has the same key and was properly replaced
                    blockData[i] = container;
                    chunkPdc.set(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY, blockData);
                }
                return;
            }
        }

        // If none was found for this block, just enter it into the array
        final PersistentDataContainer[] newData = new PersistentDataContainer[blockData.length + 1];
        System.arraycopy(blockData, 0, newData, 0, blockData.length);
        newData[blockData.length] = container;
        chunkPdc.set(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER_ARRAY, newData);
    }

    /**
     * Get the block key for a block
     *
     * @param block The block to get the key for
     * @return The block key
     */
    public static int getBlockKey(Block block) {
        final int x = block.getX() % 16;
        final int y = block.getY();
        final int z = block.getZ() % 16;
        return y & 0xFFFF | (x & 0xFF) << 16 | (z & 0xFF) << 24;
    }

}
