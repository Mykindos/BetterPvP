package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Map;

@Singleton
@ItemKey("core:elderwood_core")
public class ElderwoodCore extends BaseItem {

    private transient boolean registered;

    @Inject
    private ElderwoodCore() {
        super("Elderwood Core", Item.model("elderwood_core"), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }

    @Inject
    private void registerRecipe(AnvilRecipeRegistry registry, ItemRegistry itemRegistry, ItemFactory itemFactory,
                                Blackroot blackroot) {
        if (registered) return;
        registered = true;

        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        final Map<BaseItem, Integer> ingredients = Map.of(
                blackroot, 6,
                stick, 3
        );

        final AnvilRecipe recipe = new AnvilRecipe(
                ingredients,
                this,
                5,
                itemFactory
        );
        registry.registerRecipe(new NamespacedKey("core", "elderwood_core"), recipe);
    }
}

