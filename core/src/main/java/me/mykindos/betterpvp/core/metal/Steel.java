package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Color;

public class Steel {

    @Singleton
    public static class Alloy extends me.mykindos.betterpvp.core.recipe.smelting.Alloy {
        @Inject
        public Alloy(Steel.Ingot ingot) {
            super("Steel Alloy", "steel", ingot, Color.fromRGB(117, 117, 117), 950f);
        }
    }

    @Singleton
    @ItemKey("core:steel_ingot")
    public static class Ingot extends MetalItem {
        @Inject
        public Ingot() {
            super("Steel Ingot", "steel_ingot", ItemRarity.COMMON);
        }
    }

    @Singleton
    public static class Block extends MetalBlock {
        @Inject
        public Block() {
            super("steel_block", "Steel Block", "steel_block");
        }
    }

    @Singleton
    @ItemKey("core:steel_block")
    public static class BlockItem extends MetalBlockItem {
        @Inject
        public BlockItem() {
            super("Steel Block", "steel_block", ItemRarity.COMMON);
        }
    }

}
