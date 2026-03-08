package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:goblin_mask")
public class GoblinMask extends BaseItem {

    @Inject
    private GoblinMask() {
        super("Goblin Mask", Item.model("goblin_mask", 1), ItemGroup.MATERIAL, ItemRarity.RARE);
    }

}
