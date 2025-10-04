package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.impl.Blackroot;
import me.mykindos.betterpvp.core.recipe.smelting.AlloyRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipeBuilder;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipeRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
public class MetalRecipeBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private ItemFactory itemFactory;
    @Inject private SmeltingRecipeRegistry recipeRegistry;
    @Inject private AlloyRegistry alloyRegistry;
    @Inject private Steel.Alloy steel;
    @Inject private Runesteel.Alloy runesteel;
    @Inject private Runesteel.Fragment runebloodOre;
    @Inject private FissureQuartz.Item fissureQuartz;
    @Inject private Blackroot blackroot;

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        // Steel
        final SmeltingRecipeBuilder steelBuilder = new SmeltingRecipeBuilder();
        steelBuilder.setPrimaryResult(steel, 1000);
        steelBuilder.addIngredient(itemFactory.getFallbackItem(Material.IRON_INGOT), 10);
        steelBuilder.addIngredient(itemFactory.getFallbackItem(Material.COAL_BLOCK), 2);
        recipeRegistry.registerRecipe(new NamespacedKey("core", "steel"), steelBuilder.build(itemFactory));
        alloyRegistry.registerAlloy(steel);

        // Runesteel
        final SmeltingRecipeBuilder runesteelBuilder = new SmeltingRecipeBuilder();
        runesteelBuilder.setPrimaryResult(runesteel, 500);
        runesteelBuilder.addIngredient(blackroot, 10);
        runesteelBuilder.addIngredient(runebloodOre, 1);
        runesteelBuilder.addIngredient(fissureQuartz, 2);
        recipeRegistry.registerRecipe(new NamespacedKey("core", "runesteel"), runesteelBuilder.build(itemFactory));
        alloyRegistry.registerAlloy(runesteel);
    }

}
