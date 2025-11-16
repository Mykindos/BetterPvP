package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:stormsteel_plate")
public class StormsteelPlate extends BaseItem {

    @Inject
    private StormsteelPlate() {
        super("Stormsteel Plate", Item.model("stormsteel_plate", 16), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
