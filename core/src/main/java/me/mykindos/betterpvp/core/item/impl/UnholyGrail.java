package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:unholy_grail")
public class UnholyGrail extends BaseItem {

    @Inject
    private UnholyGrail() {
        super("Unholy Grail", Item.model("unholy_grail", 1), ItemGroup.MATERIAL, ItemRarity.LEGENDARY);
    }
}
