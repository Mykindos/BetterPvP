package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:toxic_crystal")
public class ToxicCrystal extends BaseItem {

    @Inject
    private ToxicCrystal() {
        super("Toxic Crystal", Item.model("toxic_crystal", 64), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
    }
}
