package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:magnetic_shard")
public class MagneticShard extends BaseItem {

    @Inject
    private MagneticShard() {
        super("Magnetic Shard", Item.model("magnetic_shard", 16), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
