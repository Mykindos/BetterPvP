package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:runic_dust")
public class RunicDust extends BaseItem {

    @Inject
    private RunicDust() {
        super(translatableName("core.item.runic-dust.name"), Item.model("runic_dust", 64), ItemGroup.MATERIAL, ItemRarity.RARE);
    }
}
