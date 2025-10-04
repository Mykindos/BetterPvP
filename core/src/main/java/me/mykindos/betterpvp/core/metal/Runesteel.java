package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.ShapelessCraftingRecipe;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;

import java.util.Map;

public class Runesteel {

    @Singleton
    public static class Alloy extends me.mykindos.betterpvp.core.recipe.smelting.Alloy {
        @Inject
        public Alloy(Runesteel.Ingot ingot) {
            super("Runesteel Alloy", "runesteel", ingot, Color.fromRGB(71, 151, 255), 950f);
        }
    }

    @Singleton
    public static class OreBlock extends MetalBlock {
        @Inject
        public OreBlock() {
            super("runeblood_ore", "Runeblood Ore", "runeblood_stone_ore");
        }
    }

    @Singleton
    public static class OreBlockItem extends MetalBlockItem {
        @Inject
        public OreBlockItem() {
            super("Runeblood Ore", "runeblood_stone_ore", ItemRarity.MYTHICAL);
        }
    }

    @Singleton
    public static class Fragment extends MetalItem {
        @Inject
        public Fragment() {
            super("Runeblood Fragment", "runeblood_fragment", ItemRarity.MYTHICAL);
        }
    }

    @Singleton
    public static class Ingot extends MetalItem {
        @Inject
        public Ingot() {
            super("Runesteel Ingot", "runesteel_ingot", ItemRarity.MYTHICAL);
        }
    }

    @Singleton
    public static class Billet extends MetalItem {

        private transient boolean registered;

        @Inject
        public Billet() {
            super("Runesteel Billet", "runesteel_billet", ItemRarity.MYTHICAL);
        }

        @Inject
        private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, Runesteel.Ingot ingot) {
            if (registered) return;
            registered = true;
            Map<Integer, RecipeIngredient> ingredients = Map.of(
                    0, new RecipeIngredient(ingot, 1),
                    1, new RecipeIngredient(ingot, 1)
            );
            registry.registerRecipe(new NamespacedKey("core", "runesteel_billet"), new ShapelessCraftingRecipe(this,
                    ingredients,
                    itemFactory,
                    false));
        }
    }

    @Singleton
    public static class Block extends MetalBlock {
        @Inject
        public Block() {
            super("runesteel_block", "Steel Block", "runesteel_block");
        }
    }

    @Singleton
    public static class BlockItem extends MetalBlockItem {

        private transient boolean registered;

        @Inject
        public BlockItem() {
            super("Runesteel Block", "runesteel_block", ItemRarity.MYTHICAL);
        }

        @Inject
        private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, Runesteel.Billet billet) {
            if (registered) return;
            registered = true;
            String[] pattern = new String[] {
                    "AAA",
                    "AAA",
                    "AAA"
            };
            final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
            builder.setIngredient('A', new RecipeIngredient(billet, 1));
            registry.registerRecipe(new NamespacedKey("core", "runesteel_block"), builder.build());
        }
    }

}
