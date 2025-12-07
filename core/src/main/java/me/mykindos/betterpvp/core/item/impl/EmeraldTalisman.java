package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:emerald_talisman")
public class EmeraldTalisman extends BaseItem {

    @Inject
    private EmeraldTalisman() {
        super("Emerald Talisman", Item.model("emerald_talisman", 16), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
