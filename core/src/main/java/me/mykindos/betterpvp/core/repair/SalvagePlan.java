package me.mykindos.betterpvp.core.repair;

import lombok.Value;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;

/**
 * Immutable description of a pending salvage: the gear being consumed, the
 * reinforcement BaseItem to mint, and the rolled yield count. Produced by
 * {@link SalvageService#resolve} and executed by {@link SalvageStationListener}.
 */
@Value
public class SalvagePlan {
    ItemInstance source;
    BaseItem reinforcement;
    ItemRarity tier;
    int yield;
}
