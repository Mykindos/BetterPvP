package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class FangOfTheDeep extends BaseItem {

    @Inject
    private FangOfTheDeep() {
        super("Fang of the Deep", Item.model("fang_of_the_deep", 64), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
