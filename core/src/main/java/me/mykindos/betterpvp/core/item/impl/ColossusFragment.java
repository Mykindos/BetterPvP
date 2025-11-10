package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:colossus_fragment")
public class ColossusFragment extends BaseItem {

    @Inject
    private ColossusFragment() {
        super("Colossus Fragment", Item.model("colossus_fragment", 64), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
