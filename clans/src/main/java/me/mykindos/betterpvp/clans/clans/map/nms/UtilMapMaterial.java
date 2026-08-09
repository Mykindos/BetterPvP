package me.mykindos.betterpvp.clans.clans.map.nms;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * Resolves the map colour of a block. Every {@link Material}'s colour is derived once into {@link #COLOR_BY_MATERIAL}
 * at class-init, so the terrain sampler is a plain array read — no reflection, no NMS lookup, and safe to call from a
 * worker thread against a {@code ChunkSnapshot}.
 */
@SuppressWarnings("JavaReflectionMemberAccess")
@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilMapMaterial {

    /** {@link MapColor} id per {@link Material#ordinal()}; {@link MapColor#NONE} where a material has no colour. */
    private static final byte[] COLOR_BY_MATERIAL = new byte[Material.values().length];

    private static Field PROPERTIES_FUNCTION;
    private static Field BLOCKBEHAVIOUR_INFO;

    static {
        try {
            BLOCKBEHAVIOUR_INFO = BlockBehaviour.class.getDeclaredField("O");
            BLOCKBEHAVIOUR_INFO.setAccessible(true);

            PROPERTIES_FUNCTION = BlockBehaviour.Properties.class.getDeclaredField("b");
            PROPERTIES_FUNCTION.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            log.error("Failed to access NMS field", ex).submit();
        }
        buildColorTable();
    }

    @SuppressWarnings("unchecked")
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
                final BlockBehaviour.Properties properties = (BlockBehaviour.Properties) BLOCKBEHAVIOUR_INFO.get(block);
                final Function<BlockState, MapColor> function =
                        (Function<BlockState, MapColor>) PROPERTIES_FUNCTION.get(properties);
                final int colour = function.apply(block.defaultBlockState()).id;
                COLOR_BY_MATERIAL[material.ordinal()] = (byte) colour;
                if (colour != MapColor.NONE.id) {
                    coloured++;
                }
            } catch (IllegalAccessException | RuntimeException exception) {
                COLOR_BY_MATERIAL[material.ordinal()] = (byte) MapColor.NONE.id;
            }
        }

        // An empty table means the NMS field lookup above resolved to nothing, and every column would then read as
        // void — a map drawn entirely in the background colour. Worth saying out loud rather than rendering blank.
        if (coloured == 0) {
            log.error("Resolved no block map colours — the map will render empty. NMS field names likely changed.")
                    .submit();
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
