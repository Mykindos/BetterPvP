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
 * Reinforcement consumed to repair Legendary-tier items on the anvil.
 */
@Singleton
@ItemKey("core:anchor_stone")
public class AnchorStone extends BaseItem {

    private transient boolean registered;

    @Inject
    private AnchorStone() {
        super(translatableName("core.item.anchor-stone.name"), Item.model("anchor_stone", 64), ItemGroup.MATERIAL, ItemRarity.LEGENDARY);
        addBaseComponent(new ReinforcementComponent(ItemRarity.LEGENDARY));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                ForgeSpike forgeSpike,
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
        builder.setIngredient('P', new RecipeIngredient(forgeSpike, 1));
        registry.registerRecipe(new NamespacedKey("core", "anchor_stone"), builder.build());
    }
}
