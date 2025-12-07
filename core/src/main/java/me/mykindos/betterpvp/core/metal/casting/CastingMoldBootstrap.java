package me.mykindos.betterpvp.core.metal.casting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.metal.Runesteel;
import me.mykindos.betterpvp.core.metal.Steel;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class CastingMoldBootstrap {

    @Inject private ItemFactory itemFactory;
    @Inject private CastingMoldRecipeRegistry recipeRegistry;
    @Inject private Steel.Ingot steelIngot;
    @Inject private Steel.Alloy steelAlloy;
    @Inject private Runesteel.Ingot runesteelIngot;
    @Inject private Runesteel.Alloy runesteelAlloy;
    @Inject private IngotCastingMold ingotBase;

    private NamespacedKey key(String name) {
        return new NamespacedKey(JavaPlugin.getPlugin(Core.class), name);
    }

    public void register() {
        recipeRegistry.registerRecipe(key("steel"), new CastingMoldRecipe(ingotBase, 1000, steelAlloy, steelIngot, itemFactory));
        recipeRegistry.registerRecipe(key("runesteel"), new CastingMoldRecipe(ingotBase, 1000, runesteelAlloy, runesteelIngot, itemFactory));
    }

}
