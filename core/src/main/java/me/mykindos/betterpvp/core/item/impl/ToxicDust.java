package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:toxic_moss")
public class ToxicDust extends BaseItem {

    @Inject
    private ToxicDust() {
        super("Toxic Moss", Item.model("toxic_moss", 64), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
    }
}
