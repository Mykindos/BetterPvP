package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.repair.ReinforcementComponent;

/**
 * Reinforcement consumed to repair Mythical-tier items on the anvil. A droplet of
 * divine essence used to mend gear no mundane material could touch.
 */
@Singleton
@ItemKey("core:ichor")
public class Ichor extends BaseItem {

    @Inject
    private Ichor() {
        super("Ichor", Item.model("ichor", 64), ItemGroup.MATERIAL, ItemRarity.MYTHICAL);
        addBaseComponent(new ReinforcementComponent(ItemRarity.MYTHICAL));
    }
}
