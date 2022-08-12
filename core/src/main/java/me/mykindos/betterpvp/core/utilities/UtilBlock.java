package me.mykindos.betterpvp.core.utilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class UtilBlock {

    public static HashSet<Material> blockAirFoliageSet = new HashSet<>();
    public static HashSet<Material> blockPassSet = new HashSet<>();
    public static HashSet<Material> blockUseSet = new HashSet<>();

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
     * @param p The Player
     * @return Returns true if the Player is standing in water
     */
    public static boolean isInWater(Player p) {

        Block block = p.getLocation().getBlock();
        return block.getType() == Material.WATER
                || block.getType() == Material.BUBBLE_COLUMN;
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
     * Check if an Entity is on the ground
     *
     * @param ent The Entity
     * @return Returns true if the entity is on the ground
     */
    public static boolean isGrounded(Entity ent) {
        if ((ent instanceof CraftEntity)) {

            return ent.isOnGround();
        }
        return !airFoliage(ent.getLocation().add(0, -1, 0).getBlock());
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
        if (blockAirFoliageSet.isEmpty()) {
            blockAirFoliageSet.add(Material.AIR);
            blockAirFoliageSet.add(Material.CAVE_AIR);
            blockAirFoliageSet.add(Material.VOID_AIR);
            for (Material m : Material.values()) {
                if (m.name().contains("SAPLING")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("DEAD")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("TULIP")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("CORAL")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("_SIGN")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("_BANNER")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("CHORUS")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("TORCH")) {
                    blockAirFoliageSet.add(m);
                } else if (m.name().contains("GATE")) {
                    blockAirFoliageSet.add(m);
                }
            }

            blockAirFoliageSet.add(Material.BROWN_MUSHROOM);
            blockAirFoliageSet.add(Material.RED_MUSHROOM);
            blockAirFoliageSet.add(Material.FERN);
            blockAirFoliageSet.add(Material.LARGE_FERN);
            blockAirFoliageSet.add(Material.GRASS);
            blockAirFoliageSet.add(Material.TALL_GRASS);
            blockAirFoliageSet.add(Material.SEAGRASS);
            blockAirFoliageSet.add(Material.TALL_SEAGRASS);
            blockAirFoliageSet.add(Material.CORNFLOWER);
            blockAirFoliageSet.add(Material.SUNFLOWER);
            blockAirFoliageSet.add(Material.LILY_OF_THE_VALLEY);
            blockAirFoliageSet.add(Material.SEA_PICKLE);
            blockAirFoliageSet.add(Material.POPPY);
            blockAirFoliageSet.add(Material.OXEYE_DAISY);
            blockAirFoliageSet.add(Material.WITHER_ROSE);
            blockAirFoliageSet.add(Material.DANDELION);
            blockAirFoliageSet.add(Material.AZURE_BLUET);
            blockAirFoliageSet.add(Material.BLUE_ORCHID);
            blockAirFoliageSet.add(Material.ALLIUM);
            blockAirFoliageSet.add(Material.SNOW);

        }

        return blockAirFoliageSet.contains(mat);
    }


    /**
     * Check if block is a block entities can walk through (e.g. Long grass)
     *
     * @param block block ID
     * @return Returns true if the block does not stop player movement
     */
    public static boolean airFoliage(int block) {
        return airFoliage((byte) block);
    }

    /**
     * Check if block is a block entities can walk through (e.g. Long grass)
     *
     * @param block Block to check
     * @return Returns true if the block does not stop player movement
     */
    @SuppressWarnings("deprecation")
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
        if (blockPassSet.isEmpty()) {

            blockPassSet.add(Material.AIR);
            blockPassSet.add(Material.CAVE_AIR);
            blockPassSet.add(Material.VOID_AIR);
            for (Material m : Material.values()) {
                if (m.name().contains("SAPLING")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("DEAD")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("_BED")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("RAIL")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("TULIP")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("MUSHROOM")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("CARPET")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("CORAL")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("_SIGN")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("_BANNER")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("_HEAD")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("_SLAB")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("POTTED")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("CHORUS")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("TORCH")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("GATE")) {
                    blockPassSet.add(m);
                } else if (m.name().contains("BUTTON")) {
                    blockPassSet.add(m);
                }
            }

            blockPassSet.add(Material.GRASS);
            blockPassSet.add(Material.TALL_GRASS);
            blockPassSet.add(Material.REDSTONE_WIRE);
            blockPassSet.add(Material.CAULDRON);
            blockPassSet.add(Material.BREWING_STAND);
            blockPassSet.add(Material.WATER);
            blockPassSet.add(Material.LAVA);
            blockPassSet.add(Material.COBWEB);
            blockPassSet.add(Material.FERN);
            blockPassSet.add(Material.LARGE_FERN);
            blockPassSet.add(Material.GRASS);
            blockPassSet.add(Material.SEAGRASS);
            blockPassSet.add(Material.TALL_SEAGRASS);
            blockPassSet.add(Material.CORNFLOWER);
            blockPassSet.add(Material.SUNFLOWER);
            blockPassSet.add(Material.LILY_OF_THE_VALLEY);
            blockPassSet.add(Material.SEA_PICKLE);
            blockPassSet.add(Material.POPPY);
            blockPassSet.add(Material.OXEYE_DAISY);
            blockPassSet.add(Material.WITHER_ROSE);
            blockPassSet.add(Material.DANDELION);
            blockPassSet.add(Material.AZURE_BLUET);
            blockPassSet.add(Material.BLUE_ORCHID);
            blockPassSet.add(Material.ALLIUM);
            blockPassSet.add(Material.LANTERN);
            blockPassSet.add(Material.CAMPFIRE);
            blockPassSet.add(Material.BARRIER);

        }

        return !blockPassSet.contains(mat);
    }

    /**
     * Check if a block is a solid block
     *
     * @param block Block to check
     * @return Returns true if the block is solid (e.g. Stone)
     */
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
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
        if (blockUseSet.isEmpty()) {
            for (Material m : Material.values()) {
                if (m.name().contains("CHEST")) {
                    blockUseSet.add(m);
                } else if (m.name().contains("FURNACE")) {
                    blockUseSet.add(m);
                } else if (m.name().contains("SHULKER")) {
                    blockUseSet.add(m);
                } else if (m.name().contains("ANVIL")) {
                    blockUseSet.add(m);
                } else if (m.name().contains("DOOR")) {
                    blockUseSet.add(m);
                } else if (m.name().contains("GATE")) {
                    blockUseSet.add(m);
                }
            }

            blockUseSet.add(Material.CAULDRON);
            blockUseSet.add(Material.CARTOGRAPHY_TABLE);
            blockUseSet.add(Material.BEEHIVE);
            blockUseSet.add(Material.JUKEBOX);
            blockUseSet.add(Material.ENCHANTING_TABLE);
            blockUseSet.add(Material.SMOKER);
            blockUseSet.add(Material.BARREL);
            blockUseSet.add(Material.BREWING_STAND);
            blockUseSet.add(Material.BELL);
            blockUseSet.add(Material.SMITHING_TABLE);
            blockUseSet.add(Material.LOOM);
            blockUseSet.add(Material.GRINDSTONE);
            blockUseSet.add(Material.DISPENSER);
            blockUseSet.add(Material.IRON_DOOR);
            blockUseSet.add(Material.IRON_TRAPDOOR);
            blockUseSet.add(Material.NOTE_BLOCK);

          /*  blockUseSet.add((byte) 23);
            blockUseSet.add((byte) 330);
            blockUseSet.add((byte) 167);
            blockUseSet.add((byte) 26);
            blockUseSet.add((byte) 54);
            blockUseSet.add((byte) 58);
            blockUseSet.add((byte) 61);
            blockUseSet.add((byte) 62);
            blockUseSet.add((byte) 64);
            blockUseSet.add((byte) 69);
            blockUseSet.add((byte) 71);
            blockUseSet.add((byte) 77);
            blockUseSet.add((byte) 93);
            blockUseSet.add((byte) 94);
            blockUseSet.add((byte) 96);
            blockUseSet.add((byte) 107);
            blockUseSet.add((byte) 183);
            blockUseSet.add((byte) 184);
            blockUseSet.add((byte) 185);
            blockUseSet.add((byte) 186);
            blockUseSet.add((byte) 187);
            blockUseSet.add((byte) 117);
            blockUseSet.add((byte) 116);
            blockUseSet.add((byte) 145);
            blockUseSet.add((byte) 146);*/
        }
        return blockUseSet.contains(mat);
    }

    public static boolean isInLiquid(Entity ent) {
        Block bottomBlock = ent.getLocation().getBlock();
        Block topBlock = ent.getLocation().add(0, 1, 0).getBlock();

        if (bottomBlock.isLiquid() || bottomBlock.getType().name().contains("SEAGRASS") || bottomBlock.getType().name().contains("KELP")
                || topBlock.isLiquid() || topBlock.getType().name().contains("SEAGRASS") || bottomBlock.getType().name().contains("KELP")) {
            return true;
        }

        return false;
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
        if (name.contains("STAIRS") || name.contains("CAMPFIRE") || name.contains("SLAB") || name.contains("ICE")
                || name.contains("WIRE") || name.contains("FENCE")) {
            return false;
        }

        return true;
    }
}
