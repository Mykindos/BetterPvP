package me.mykindos.betterpvp.core.loot.chest;

import me.mykindos.betterpvp.core.utilities.model.SoundEffect;

public final class BigLootChest extends LootChest {

    public BigLootChest() {
        this("bpvp_loot_chest");
    }

    public BigLootChest(String mythicMobName) {
        super(mythicMobName, new SoundEffect("littleroom_piratepack", "littleroom.piratepack.captain_chest_item"), 30, 10);
    }
}
