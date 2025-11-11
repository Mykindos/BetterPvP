package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:runic_plate")
public class RunicPlate extends BaseItem {

    @Inject
    private RunicPlate() {
        super("Runic Plate", Item.model("runic_plate", 16), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
    }
}
