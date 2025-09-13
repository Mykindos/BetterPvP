package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.impl.Blackroot;
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
    private MetalRecipeBootstrap(ItemFactory itemFactory, SmeltingRecipeRegistry registry,
                                 AlloyRegistry alloyRegistry) {
        this.itemFactory = itemFactory;
        this.recipeRegistry = registry;
        this.alloyRegistry = alloyRegistry;
    }

    @Inject
    private void registerSteel(Steel.Alloy steel) {
        final SmeltingRecipeBuilder builder = new SmeltingRecipeBuilder();
        builder.setPrimaryResult(steel, 1000);
        builder.addIngredient(itemFactory.getFallbackItem(Material.IRON_INGOT), 10);
        builder.addIngredient(itemFactory.getFallbackItem(Material.COAL_BLOCK), 2);
        recipeRegistry.registerRecipe(builder.build(itemFactory));
        alloyRegistry.registerAlloy(steel);
    }

    @Inject
    private void registerRunesteel(Runesteel.Alloy runesteel, Runesteel.Fragment runebloodOre,
                                   FissureQuartz.Item fissureQuartz, Blackroot blackroot) {
        final SmeltingRecipeBuilder builder = new SmeltingRecipeBuilder();
        builder.setPrimaryResult(runesteel, 500);
        builder.addIngredient(blackroot, 10);
        builder.addIngredient(runebloodOre, 1);
        builder.addIngredient(fissureQuartz, 2);
        recipeRegistry.registerRecipe(builder.build(itemFactory));
        alloyRegistry.registerAlloy(runesteel);
    }

}
