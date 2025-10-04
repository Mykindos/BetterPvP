package me.mykindos.betterpvp.core.loot.chest;

import com.google.inject.Singleton;
import io.lumine.mythic.core.mobs.ActiveMob;
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
        return lootChests.stream()
                .filter(lootChest -> lootChest.getActiveMob().getEntity().getBukkitEntity().equals(entity))
                .findFirst()
                .orElse(null);
    }

    public LootChest getLootChest(ActiveMob mob) {
        return lootChests.stream()
                .filter(lootChest -> lootChest.getActiveMob().equals(mob))
                .findFirst()
                .orElse(null);
    }

}
