package me.mykindos.betterpvp.core.loot.chest;

import com.google.inject.Singleton;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.Getter;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;

@Singleton
@Getter
public class LootChestManager {

    private final Map<ActiveMob, LootChest> lootChests = new HashMap<>();

    void addLootChest(LootChest lootChest, ActiveMob mob) {
        lootChests.put(mob, lootChest);
    }

    public LootChest getLootChest(Entity entity) {
        return lootChests.entrySet().stream()
                .filter(entry -> entry.getKey().getEntity().getBukkitEntity().equals(entity))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public LootChest getLootChest(ActiveMob mob) {
        return lootChests.get(mob);
    }

}
