package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class ReapersEdge extends BaseItem {

    @Inject
    private ReapersEdge() {
        super("Reaper's Edge", Item.model("reapers_edge", 64), ItemGroup.MATERIAL, ItemRarity.LEGENDARY);
    }
}
