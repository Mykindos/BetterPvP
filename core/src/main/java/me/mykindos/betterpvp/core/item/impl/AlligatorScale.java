package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class AlligatorScale extends BaseItem {

    @Inject
    private AlligatorScale() {
        super("Alligator Scale", Item.model("alligator_scale", 64), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
