package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.repair.ReinforcementComponent;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.NamespacedKey;

/**
 * Reinforcement consumed to repair Uncommon-tier items on the anvil.
 */
@Singleton
@ItemKey("core:bronze_plate")
public class BronzePlate extends BaseItem {

    private transient boolean registered;

    @Inject
    private BronzePlate() {
        super(translatableName("core.item.bronze-plate.name"), Item.model("bronze_plate", 64), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
        addBaseComponent(new ReinforcementComponent(ItemRarity.UNCOMMON));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                IronPlate ironPlate,
                                RunicDust runicDust) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "DDD",
                "DPD",
                "DDD",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('D', new RecipeIngredient(runicDust, 1));
        builder.setIngredient('P', new RecipeIngredient(ironPlate, 1));
        registry.registerRecipe(new NamespacedKey("core", "bronze_plate"), builder.build());
    }
}
