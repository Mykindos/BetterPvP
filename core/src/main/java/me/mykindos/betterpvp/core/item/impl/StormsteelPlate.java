package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class StormsteelPlate extends BaseItem {

    @Inject
    private StormsteelPlate() {
        super("StormsteelPlate", Item.model("stormsteel_plate", 16), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
