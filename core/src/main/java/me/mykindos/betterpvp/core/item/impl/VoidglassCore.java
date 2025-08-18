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

import java.util.Map;

@Singleton
public class VoidglassCore extends BaseItem {

    @Inject
    private VoidglassCore() {
        super("Voidglass Core", Item.model("voidglass_core"), ItemGroup.MATERIAL, ItemRarity.MYTHICAL);
    }

}

