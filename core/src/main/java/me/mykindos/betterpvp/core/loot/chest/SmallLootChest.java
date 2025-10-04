package me.mykindos.betterpvp.core.loot.chest;

import me.mykindos.betterpvp.core.utilities.model.SoundEffect;

public final class SmallLootChest extends LootChest {

    public SmallLootChest() {
        this("Chest_Wooden");
    }

    public SmallLootChest(String mythicMobName) {
        super(mythicMobName, new SoundEffect("betterpvp", "chest.drop-item"), 15, 4);
    }
}
