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
 * Reinforcement consumed to repair Rare-tier items on the anvil.
 */
@Singleton
@ItemKey("core:tempered_pin")
public class TemperedPin extends BaseItem {

    @Inject
    private TemperedPin() {
        super("Tempered Pin", Item.model("tempered_pin", 64), ItemGroup.MATERIAL, ItemRarity.RARE);
        addBaseComponent(new ReinforcementComponent(ItemRarity.RARE));
    }
}
