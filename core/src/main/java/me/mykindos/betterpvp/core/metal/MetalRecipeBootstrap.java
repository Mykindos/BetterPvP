package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.recipe.smelting.AlloyRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipeBuilder;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipeRegistry;
import org.bukkit.Material;

@Singleton
@PluginAdapter("Core")
public class MetalRecipeBootstrap {

    private final ItemFactory itemFactory;
    private final SmeltingRecipeRegistry recipeRegistry;
    private final AlloyRegistry alloyRegistry;

    @Inject
    private MetalRecipeBootstrap(ItemFactory itemFactory, SmeltingRecipeRegistry registry, AlloyRegistry alloyRegistry) {
        this.itemFactory = itemFactory;
        this.recipeRegistry = registry;
        this.alloyRegistry = alloyRegistry;
    }

    @Inject
    private void registerSteel(Steel.Alloy steel) {
        final SmeltingRecipeBuilder builder = new SmeltingRecipeBuilder();
        builder.setPrimaryResult(steel, 500);
        builder.addIngredient(itemFactory.getBaseItem(Material.IRON_INGOT), 10);
        builder.addIngredient(itemFactory.getBaseItem(Material.COAL_BLOCK), 2);
        recipeRegistry.registerSmeltingRecipe(builder.build(itemFactory));
        alloyRegistry.registerAlloy(steel);
    }

}
