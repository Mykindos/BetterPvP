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
 * Reinforcement consumed to repair Epic-tier items on the anvil.
 */
@Singleton
@ItemKey("core:forge_spike")
public class ForgeSpike extends BaseItem {

    @Inject
    private ForgeSpike() {
        super("Forge Spike", Item.model("forge_spike", 64), ItemGroup.MATERIAL, ItemRarity.EPIC);
        addBaseComponent(new ReinforcementComponent(ItemRarity.EPIC));
    }
}
