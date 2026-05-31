package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.ruby;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.gems.GemItem;

@Singleton
@ItemKey("core:flawless_ruby")
public class RubyGemItem extends GemItem {

    @Inject
    public RubyGemItem(RubyGem gem) {
        super(gem, Item.builder("flawless_ruby").build(), ItemRarity.MYTHICAL);
    }
}
