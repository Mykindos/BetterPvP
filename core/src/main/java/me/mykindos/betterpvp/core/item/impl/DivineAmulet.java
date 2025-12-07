package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:divine_amulet")
public class DivineAmulet extends BaseItem {

    @Inject
    private DivineAmulet() {
        super("Divine Amulet", Item.model("divine_amulet", 16), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
