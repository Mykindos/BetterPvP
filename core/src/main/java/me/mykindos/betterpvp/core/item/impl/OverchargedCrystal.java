package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:overcharged_crystal")
public class OverchargedCrystal extends BaseItem {

    @Inject
    private OverchargedCrystal() {
        super("Overcharged Crystal", Item.model("overcharged_crystal", 16), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
