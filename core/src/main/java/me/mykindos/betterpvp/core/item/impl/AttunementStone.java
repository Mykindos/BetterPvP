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
        super(translatableName("core.item.attunement-stone.name"), Item.model("attunement_stone", 1), ItemGroup.MATERIAL, ItemRarity.RARE);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                MagicSeal magicSeal,
                                RunicDust runicDust) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "GDG",
                "DMD",
                "GDG",
        };
        final BaseItem goldIngot = itemFactory.getFallbackItem(Material.GOLD_INGOT);
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('G', new RecipeIngredient(goldIngot, 1));
        builder.setIngredient('D', new RecipeIngredient(runicDust, 1));
        builder.setIngredient('M', new RecipeIngredient(magicSeal, 1));
        registry.registerRecipe(new NamespacedKey("core", "attunement_stone"), builder.build());
    }
}