package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.sapphire;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.gems.GemItem;

@Singleton
@ItemKey("core:flawless_sapphire")
public class SapphireGemItem extends GemItem {

    @Inject
    public SapphireGemItem(SapphireGem gem) {
        super(gem, Item.builder("flawless_sapphire").build(), ItemRarity.MYTHICAL);
    }
}
