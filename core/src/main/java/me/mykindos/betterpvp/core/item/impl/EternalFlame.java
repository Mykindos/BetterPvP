package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class EternalFlame extends BaseItem {

    @Inject
    private EternalFlame() {
        super("Eternal Flame", Item.model("eternal_flame", 1), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
