package me.mykindos.betterpvp.core.utilities.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.listener.EntityLootController;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class MythicMobUtils {

    public static ActiveMob spawnLootMob(@NotNull String mobName, @NotNull Location location, @NotNull LootTable lootTable) {
        final ActiveMob mob = MythicBukkit.inst().getMobManager().spawnMob(mobName, location);
        if (mob != null) {
            EntityLootController.bind(((LivingEntity) mob.getEntity().getBukkitEntity()), lootTable);
        }
        return mob;
    }

}
