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
 * Reinforcement consumed to repair Common-tier items on the anvil.
 */
@Singleton
@ItemKey("core:iron_plate")
public class IronPlate extends BaseItem {

    @Inject
    private IronPlate() {
        super("Iron Plate", Item.model("iron_plate", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);
        addBaseComponent(new ReinforcementComponent(ItemRarity.COMMON));
    }
}
