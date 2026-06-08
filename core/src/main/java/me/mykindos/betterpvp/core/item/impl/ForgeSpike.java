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
 * Reinforcement consumed to repair Epic-tier items on the anvil.
 */
@Singleton
@ItemKey("core:forge_spike")
public class ForgeSpike extends BaseItem {

    private transient boolean registered;

    @Inject
    private ForgeSpike() {
        super(translatableName("core.item.forge-spike.name"), Item.model("forge_spike", 64), ItemGroup.MATERIAL, ItemRarity.EPIC);
        addBaseComponent(new ReinforcementComponent(ItemRarity.EPIC));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                TemperedPin temperedPin,
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
        builder.setIngredient('P', new RecipeIngredient(temperedPin, 1));
        registry.registerRecipe(new NamespacedKey("core", "forge_spike"), builder.build());
    }
}
