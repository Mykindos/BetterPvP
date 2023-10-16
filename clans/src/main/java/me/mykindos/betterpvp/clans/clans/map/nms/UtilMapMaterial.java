package me.mykindos.betterpvp.clans.clans.map.nms;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftMagicNumbers;

import java.lang.reflect.Field;
import java.util.function.Function;

public class UtilMapMaterial {

    private static Field PROPERTIES_FUNCTION;
    private static Field BLOCKBEHAVIOUR_INFO;

    static {
        try {

            BLOCKBEHAVIOUR_INFO = BlockBehaviour.class.getDeclaredField("aN");
            BLOCKBEHAVIOUR_INFO.setAccessible(true);

            PROPERTIES_FUNCTION = BlockBehaviour.Properties.class.getDeclaredField("a");
            PROPERTIES_FUNCTION.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }


    public static MapColor getColorNeutral() {
        return MapColor.COLOR_YELLOW;
    }

    @SuppressWarnings("unchecked")
    public static MapColor getBlockColor(org.bukkit.block.Block block) {
        try {
            Block craftBlock = CraftMagicNumbers.getBlock(block.getType());
            BlockBehaviour.Properties properties = (BlockBehaviour.Properties) BLOCKBEHAVIOUR_INFO.get(craftBlock);


            Function<BlockState, MapColor> function = (Function<BlockState, MapColor>) PROPERTIES_FUNCTION.get(properties);
            int colour = function.apply(craftBlock.defaultBlockState()).id;

            return MapColor.byId(colour);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}
