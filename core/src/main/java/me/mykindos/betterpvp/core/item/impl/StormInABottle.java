package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:storm_in_a_bottle")
public class StormInABottle extends BaseItem {

    @Inject
    private StormInABottle() {
        super("Storm in a Bottle", Item.model("storm_in_a_bottle"), ItemGroup.MATERIAL, ItemRarity.MYTHICAL);
    }

}

