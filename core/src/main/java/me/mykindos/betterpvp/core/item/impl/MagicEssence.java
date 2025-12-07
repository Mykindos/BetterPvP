package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:magic_essence")
public class MagicEssence extends BaseItem {

    @Inject
    private MagicEssence() {
        super("Magic Essence", Item.model("magic_essence", 64), ItemGroup.MATERIAL, ItemRarity.RARE);
    }
}
