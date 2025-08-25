package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class AetherCore extends BaseItem {

    @Inject
    private AetherCore() {
        super("Aether Core", Item.model("aether_core", 1), ItemGroup.MATERIAL, ItemRarity.LEGENDARY);
    }
}
