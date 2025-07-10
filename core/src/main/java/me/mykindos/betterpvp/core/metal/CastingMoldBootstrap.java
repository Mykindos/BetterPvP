package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeResult;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.metal.casting.FullCastingMold;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

@Singleton
@PluginAdapter("Core")
public class CastingMoldBootstrap {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final CastingMoldRecipeRegistry recipeRegistry;
    private final AnvilRecipeRegistry anvilRecipeRegistry;

    @Inject
    private CastingMoldBootstrap(ItemFactory itemFactory, ItemRegistry itemRegistry, CastingMoldRecipeRegistry recipeRegistry, AnvilRecipeRegistry anvilRecipeRegistry) {
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        this.recipeRegistry = recipeRegistry;
        this.anvilRecipeRegistry = anvilRecipeRegistry;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(JavaPlugin.getPlugin(Core.class), name);
    }

    @Inject
    private void registerBase() {
        final CastingMold base = new CastingMold("Casting Mold", "base");
        itemRegistry.registerItem(key("casting_mold"), base);
    }

    @Inject
    private void registerIngots(Steel.Ingot steelIngot, Steel.Alloy steelAlloy) {
        final CastingMold ingotBase = new CastingMold("Ingot Casting Mold", "ingot");
        CastingMoldRecipe ingotMoldRecipe = new CastingMoldRecipe(ingotBase, 1000); // 1000 mB required for ingot molds
        itemRegistry.registerItem(key("ingot_casting_mold"), ingotBase);

        // Steel
        FullCastingMold steelFilledMold = new FullCastingMold("ingot", "steel", ingotBase, steelIngot);
        itemRegistry.registerItem(key("ingot_casting_mold_steel"), steelFilledMold);

        // Register the casting mold recipe
        ingotMoldRecipe.addAcceptedAlloy(steelAlloy, steelFilledMold);
        recipeRegistry.registerRecipe(ingotMoldRecipe);

        // Register anvil recipe for separating the filled mold
        registerMoldSeparationRecipe(steelFilledMold);
    }

    /**
     * Registers an anvil recipe for separating a full casting mold.
     * Takes 1 full casting mold and 3 hammer swings to produce the yield and empty mold.
     * @param fullCastingMold The full casting mold to create a separation recipe for
     */
    private void registerMoldSeparationRecipe(FullCastingMold fullCastingMold) {
        // Create ingredient map: 1 full casting mold
        Map<BaseItem, Integer> ingredients = Map.of(fullCastingMold, 1);

        // Create result: primary = yield, secondary = empty mold
        AnvilRecipeResult result = new AnvilRecipeResult(
                fullCastingMold.getYield(),
                List.of(fullCastingMold.getEmptyMold())
        );

        // Create and register the anvil recipe
        AnvilRecipe separationRecipe = new AnvilRecipe(
                ingredients,
                result,
                3, // 3 hammer swings required
                itemFactory
        );

        anvilRecipeRegistry.registerRecipe(separationRecipe);
    }

}
