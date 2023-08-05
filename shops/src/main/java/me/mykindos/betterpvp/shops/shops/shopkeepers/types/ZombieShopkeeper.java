package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import org.bukkit.Location;


public class ZombieShopkeeper extends StandardShopkeeper {
    public ZombieShopkeeper(Location location, String name) {
        super(EntityType.ZOMBIE, location, name);
    }

    public ZombieShopkeeper(EntityType<? extends Zombie> type, Location location, String name) {
        super(type, location, name);
    }

}
