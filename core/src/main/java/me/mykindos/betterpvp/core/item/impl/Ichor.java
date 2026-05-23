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
 * Reinforcement consumed to repair Mythical-tier items on the anvil. A droplet of
 * divine essence used to mend gear no mundane material could touch.
 */
@Singleton
@ItemKey("core:ichor")
public class Ichor extends BaseItem {

    private transient boolean registered;

    @Inject
    private Ichor() {
        super("Ichor", Item.model("ichor", 64), ItemGroup.MATERIAL, ItemRarity.MYTHICAL);
        addBaseComponent(new ReinforcementComponent(ItemRarity.MYTHICAL));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                AnchorStone anchorStone,
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
        builder.setIngredient('P', new RecipeIngredient(anchorStone, 1));
        registry.registerRecipe(new NamespacedKey("core", "ichor"), builder.build());
    }
}
