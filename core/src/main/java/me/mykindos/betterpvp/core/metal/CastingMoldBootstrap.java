package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@PluginAdapter("Core")
public class CastingMoldBootstrap {

    private final ItemRegistry itemRegistry;
    private final CastingMoldRecipeRegistry recipeRegistry;

    @Inject
    private CastingMoldBootstrap(ItemRegistry itemRegistry, CastingMoldRecipeRegistry recipeRegistry) {
        this.itemRegistry = itemRegistry;
        this.recipeRegistry = recipeRegistry;
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
    private void registerIngots(Steel.Ingot steelIngot, Steel.Alloy steelAlloy,
                                Runesteel.Ingot runesteelIngot, Runesteel.Alloy runesteelAlloy) {
        final CastingMold ingotBase = new CastingMold("Ingot Casting Mold", "ingot");
        CastingMoldRecipe ingotMoldRecipe = new CastingMoldRecipe(ingotBase, 1000); // 1000 mB required for ingot molds
        itemRegistry.registerItem(key("ingot_casting_mold"), ingotBase);

        ingotMoldRecipe.addAcceptedAlloy(steelAlloy, steelIngot);
        ingotMoldRecipe.addAcceptedAlloy(runesteelAlloy, runesteelIngot);

        recipeRegistry.registerRecipe(ingotMoldRecipe);
    }

}
