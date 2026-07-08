package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import net.kyori.adventure.text.Component;

@Singleton
@ItemKey("core:mirror_of_kalandra")
public class MirrorOfKalandra extends BaseItem {

    @Inject
    private MirrorOfKalandra() {
        super("Mirror of Kalandra", Item.model("mirror_of_kalandra", 1), ItemGroup.MATERIAL, ItemRarity.MYTHICAL);
        addBaseComponent(new DescriptionComponent(1,
                Component.text("Duplicate any item by placing it alongside this mirror in an Imbuement Pedestal.")));
    }
}
