package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

public class FissureQuartz {

    @Singleton
    @ItemKey("core:fissure_quartz")
    public static class Item extends MetalItem {
        @Inject
        public Item() {
            super("Fissure Quartz", "fissure_quartz", ItemRarity.RARE);
        }
    }

    @Singleton
    public static class Ore extends MetalBlock {
        @Inject
        public Ore() {
            super("fissure_quartz_ore", "Fissure Quartz Ore", "fissure_quartz_stone_ore");
        }
    }

    @Singleton
    @ItemKey("core:fissure_quartz_stone_ore")
    public static class OreItem extends MetalBlockItem {
        @Inject
        public OreItem() {
            super("Fissure Quartz Ore", "fissure_quartz_stone_ore", ItemRarity.RARE);
        }
    }

    @Singleton
    public static class DeepslateOre extends MetalBlock {
        @Inject
        public DeepslateOre() {
            super("fissure_quartz_deepslate_ore", "Fissure Quartz Deepslate Ore", "fissure_quartz_deepslate_ore");
        }
    }

    @Singleton
    @ItemKey("core:fissure_quartz_deepslate_ore")
    public static class DeepslateOreItem extends MetalBlockItem {
        @Inject
        public DeepslateOreItem() {
            super("Fissure Quartz Deepslate Ore", "fissure_quartz_deepslate_ore", ItemRarity.RARE);
        }
    }

}
