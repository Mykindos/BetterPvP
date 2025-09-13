package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@PluginAdapter("Core")
public class CastingMoldBootstrap {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final CastingMoldRecipeRegistry recipeRegistry;

    @Inject
    private CastingMoldBootstrap(ItemFactory itemFactory, ItemRegistry itemRegistry, CastingMoldRecipeRegistry recipeRegistry) {
        this.itemFactory = itemFactory;
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
        itemRegistry.registerItem(key("ingot_casting_mold"), ingotBase);
        recipeRegistry.registerRecipe(new CastingMoldRecipe(ingotBase, 1000, steelAlloy, steelIngot, itemFactory));
        recipeRegistry.registerRecipe(new CastingMoldRecipe(ingotBase, 1000, runesteelAlloy, runesteelIngot, itemFactory));
    }

}
