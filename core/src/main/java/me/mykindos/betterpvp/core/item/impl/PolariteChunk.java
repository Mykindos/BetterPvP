package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class PolariteChunk extends BaseItem {

    @Inject
    private PolariteChunk() {
        super("Polarite Chunk", Item.model("polarite_chunk", 64), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
