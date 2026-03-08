package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:voidglass_core")
public class VoidglassCore extends BaseItem {

    @Inject
    private VoidglassCore() {
        super("Voidglass Core", Item.model("voidglass_core"), ItemGroup.MATERIAL, ItemRarity.MYTHICAL);
    }

}

