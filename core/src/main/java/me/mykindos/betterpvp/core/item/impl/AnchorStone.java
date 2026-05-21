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
 * Reinforcement consumed to repair Legendary-tier items on the anvil.
 */
@Singleton
@ItemKey("core:anchor_stone")
public class AnchorStone extends BaseItem {

    @Inject
    private AnchorStone() {
        super("Anchor Stone", Item.model("anchor_stone", 64), ItemGroup.MATERIAL, ItemRarity.LEGENDARY);
        addBaseComponent(new ReinforcementComponent(ItemRarity.LEGENDARY));
    }
}
