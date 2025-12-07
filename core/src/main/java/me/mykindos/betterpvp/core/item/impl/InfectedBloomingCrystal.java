package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:infected_blooming_crystal")
public class InfectedBloomingCrystal extends BaseItem {

    @Inject
    private InfectedBloomingCrystal() {
        super("Infected Blooming Crystal", Item.model("infected_blooming_crystal", 64), ItemGroup.MATERIAL, ItemRarity.RARE);
    }

}
