package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class CastingMoldBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private ItemFactory itemFactory;
    @Inject private CastingMoldRecipeRegistry recipeRegistry;
    @Inject private Steel.Ingot steelIngot;
    @Inject private Steel.Alloy steelAlloy;
    @Inject private Runesteel.Ingot runesteelIngot;
    @Inject private Runesteel.Alloy runesteelAlloy;

    private NamespacedKey key(String name) {
        return new NamespacedKey(JavaPlugin.getPlugin(Core.class), name);
    }

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        // Base
        final CastingMold base = new CastingMold("Casting Mold", "base");
        itemRegistry.registerItem(key("casting_mold"), base);

        // Ingots
        final CastingMold ingotBase = new CastingMold("Ingot Casting Mold", "ingot");
        itemRegistry.registerItem(key("ingot_casting_mold"), ingotBase);
        recipeRegistry.registerRecipe(new NamespacedKey("core", "steel"), new CastingMoldRecipe(ingotBase, 1000, steelAlloy, steelIngot, itemFactory));
        recipeRegistry.registerRecipe(new NamespacedKey("core", "runesteel"), new CastingMoldRecipe(ingotBase, 1000, runesteelAlloy, runesteelIngot, itemFactory));
    }

}
