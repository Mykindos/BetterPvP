package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.emerald;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.gems.GemItem;

@Singleton
@ItemKey("core:flawless_emerald")
public class EmeraldGemItem extends GemItem {

    @Inject
    public EmeraldGemItem(EmeraldGem gem) {
        super(gem, Item.builder("flawless_emerald").build(), ItemRarity.MYTHICAL);
    }
}
