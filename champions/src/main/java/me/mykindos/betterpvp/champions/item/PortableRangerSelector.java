package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.impl.Rope;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@ItemKey("champions:portable_ranger_selector")
@Singleton
public class PortableRangerSelector extends PortableClassSelectorItem {

    private transient boolean registered;

    @Inject
    private PortableRangerSelector(Champions champions) {
        super(champions, Role.RANGER, Material.LIME_HARNESS);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory, Rope rope) {
        if (registered) return;
        registered = true;
        final BaseItem ore = itemFactory.getFallbackItem(Material.EMERALD);
        String[] pattern = new String[] {
                "IRI",
                "IRI",
                "IRI",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('R', new RecipeIngredient(rope, 1));
        builder.setIngredient('I', new RecipeIngredient(ore, 1));
        registry.registerRecipe(new NamespacedKey("champions", "portable_ranger_selector"), builder.build());
    }
}
