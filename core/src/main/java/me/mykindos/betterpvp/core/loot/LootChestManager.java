package me.mykindos.betterpvp.core.loot;

import com.google.inject.Singleton;
import lombok.Getter;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Getter
public class LootChestManager {

    private final List<LootChest> lootChests = new ArrayList<>();

    public void addLootChest(LootChest lootChest){
        lootChests.add(lootChest);
    }

    public LootChest getLootChest(Entity entity) {
        return lootChests.stream().filter(lootChest -> lootChest.getEntity().equals(entity)).findFirst().orElse(null);
    }

}
