package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:goblin_blood")
public class GoblinBlood extends BaseItem {

    @Inject
    private GoblinBlood() {
        super("Goblin Blood", Item.model("goblin_blood", 1), ItemGroup.MATERIAL, ItemRarity.RARE);
    }
}
