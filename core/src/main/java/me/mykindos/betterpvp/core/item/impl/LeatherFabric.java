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

/**
 * The block tier of leather, giving assassin armor the same ingot/block split the ore-backed roles have.
 */
@Singleton
@ItemKey("core:leather_fabric")
public class LeatherFabric extends BaseItem {

    private transient boolean registered;

    @Inject
    private LeatherFabric() {
        super(translatableName("core.item.leather-fabric.name"), Item.model("leather_fabric", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem leather = itemFactory.getFallbackItem(Material.LEATHER);

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, new String[] {
                "LLL",
                "LLL",
                "LLL",
        }, itemFactory);
        builder.setIngredient('L', new RecipeIngredient(leather, 1));
        registry.registerRecipe(new NamespacedKey("core", "leather_fabric"), builder.build());
    }
}
