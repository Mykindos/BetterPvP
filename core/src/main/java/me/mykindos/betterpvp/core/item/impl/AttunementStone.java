package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
@ItemKey("core:attunement_stone")
public class AttunementStone extends BaseItem {
    private transient boolean registered;

    @Inject
    private AttunementStone() {
        super("Attunement Stone", Item.model("attunement_stone", 1), ItemGroup.MATERIAL, ItemRarity.RARE);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                MagicEssence magicEssence) {
        if (registered) return;
        registered = true;

        String[] pattern = new String[] {
                "IBI",
                "BEB",
                "IBI",
        };

        final BaseItem ingot = itemFactory.getFallbackItem(Material.GOLD_INGOT);
        final BaseItem block = itemFactory.getFallbackItem(Material.GOLD_BLOCK);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('I', new RecipeIngredient(ingot, 1));
        builder.setIngredient('B', new RecipeIngredient(block, 1));
        builder.setIngredient('E', new RecipeIngredient(magicEssence, 1));
        
        registry.registerRecipe(new NamespacedKey("core", "attunement_stone"), builder.build());
    }
}
