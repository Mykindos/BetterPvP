package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeResult;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;

import java.util.Map;

@Singleton
public class DurakHandle extends BaseItem {

    @Inject
    private DurakHandle() {
        super("Durak Handle", Item.model("durak_handle"), ItemGroup.MATERIAL, ItemRarity.LEGENDARY);
    }

    @Inject
    private void registerRecipe(AnvilRecipeRegistry registry, ItemRegistry itemRegistry, ItemFactory itemFactory,
                                Duskhide duskhide, Blackroot blackroot, MagicSeal magicSeal) {
        final Map<BaseItem, Integer> ingredients = Map.of(
                duskhide, 3,
                blackroot, 10,
                magicSeal, 1
        );

        final AnvilRecipe recipe = new AnvilRecipe(
                ingredients,
                new AnvilRecipeResult(this),
                5,
                itemFactory
        );
        registry.registerRecipe(new NamespacedKey("core", "durak_handle"), recipe);
    }
}

