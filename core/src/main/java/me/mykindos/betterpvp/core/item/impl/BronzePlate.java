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
 * Reinforcement consumed to repair Uncommon-tier items on the anvil.
 */
@Singleton
@ItemKey("core:bronze_plate")
public class BronzePlate extends BaseItem {

    @Inject
    private BronzePlate() {
        super("Bronze Plate", Item.model("bronze_plate", 64), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
        addBaseComponent(new ReinforcementComponent(ItemRarity.UNCOMMON));
    }
}
