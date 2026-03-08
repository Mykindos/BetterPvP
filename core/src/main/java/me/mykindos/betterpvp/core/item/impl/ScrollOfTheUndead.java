package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("core:scroll_of_the_undead")
public class ScrollOfTheUndead extends BaseItem {

    @Inject
    private ScrollOfTheUndead() {
        super("Scroll of the Undead", Item.model("scroll_of_the_undead"), ItemGroup.MATERIAL, ItemRarity.LEGENDARY);
    }

}

