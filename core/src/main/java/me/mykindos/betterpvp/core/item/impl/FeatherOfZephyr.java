package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class FeatherOfZephyr extends BaseItem {

    @Inject
    private FeatherOfZephyr() {
        super("Feather of Zephyr", Item.model("feather_of_zephyr", 64), ItemGroup.MATERIAL, ItemRarity.EPIC);
    }
}
