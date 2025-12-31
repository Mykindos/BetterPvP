package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:attunement_stone")
public class AttunementStone extends BaseItem {

    @Inject
    private AttunementStone() {
        super("Attunement Stone", Item.model("attunement_stone", 1), ItemGroup.MATERIAL, ItemRarity.RARE);
    }
}
