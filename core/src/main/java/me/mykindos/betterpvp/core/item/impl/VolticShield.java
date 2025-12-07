package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:voltic_shield")
public class VolticShield extends BaseItem {

    @Inject
    private VolticShield() {
        super("Voltic Shield", Item.model("voltic_shield", 1), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
