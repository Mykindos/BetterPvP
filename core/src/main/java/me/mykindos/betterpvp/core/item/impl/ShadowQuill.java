package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
public class ShadowQuill extends BaseItem {

    @Inject
    private ShadowQuill() {
        super("Shadow Quill", Item.model("shadow_quill", 64), ItemGroup.MATERIAL, ItemRarity.RARE);
    }
}
