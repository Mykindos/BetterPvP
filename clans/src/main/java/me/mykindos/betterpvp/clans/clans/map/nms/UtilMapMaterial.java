package me.mykindos.betterpvp.clans.clans.map.nms;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

/**
 * Resolves the map colour of a block. Every {@link Material}'s colour is derived once into {@link #COLOR_BY_MATERIAL}
 * at class-init, so the terrain sampler is a plain array read, safe to call from a
 * worker thread against a {@code ChunkSnapshot}.
 */
@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilMapMaterial {

    /** {@link MapColor} id per {@link Material#ordinal()}; {@link MapColor#NONE} where a material has no colour. */
    private static final byte[] COLOR_BY_MATERIAL = new byte[Material.values().length];

    static {
        buildColorTable();
    }

    private static void buildColorTable() {
        int coloured = 0;
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isBlock()) {
                continue;
            }
            try {
                final Block block = CraftMagicNumbers.getBlock(material);
                if (block == null) {
                    continue;
                }
                final int colour = block.defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).id;
                COLOR_BY_MATERIAL[material.ordinal()] = (byte) colour;
                if (colour != MapColor.NONE.id) {
                    coloured++;
                }
            } catch (RuntimeException exception) {
                COLOR_BY_MATERIAL[material.ordinal()] = (byte) MapColor.NONE.id;
            }
        }

        // An empty table would draw every column as void, a map entirely in the background colour.
        if (coloured == 0) {
            log.error("Resolved no block map colours, so the map will render empty.").submit();
        }
    }

    public static MapColor getColorNeutral() {
        return MapColor.COLOR_YELLOW;
    }

    /**
     * @param material any material
     * @return its map colour id, or {@link MapColor#NONE}'s id ({@code 0}) if it does not paint the map
     */
    public static int getColorId(Material material) {
        return COLOR_BY_MATERIAL[material.ordinal()] & 0xFF;
    }

    /**
     * Resolves a block's colour, descending through colourless blocks (glass, air pockets) exactly as the map does.
     */
    public static MapColor getBlockColor(org.bukkit.block.Block block) {
        int colour = getColorId(block.getType());
        if (colour == MapColor.NONE.id) {
            final org.bukkit.block.Block below = block.getRelative(BlockFace.DOWN);
            if (below.getY() >= block.getWorld().getMinHeight()) {
                return getBlockColor(below);
            }
        }
        return MapColor.byId(colour);
    }
}
