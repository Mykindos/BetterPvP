package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.diamond;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.gems.GemItem;

@Singleton
@ItemKey("core:flawless_diamond")
public class DiamondGemItem extends GemItem {

    @Inject
    public DiamondGemItem(DiamondGem gem) {
        super(gem, Item.builder("flawless_diamond").build(), ItemRarity.MYTHICAL);
    }
}
