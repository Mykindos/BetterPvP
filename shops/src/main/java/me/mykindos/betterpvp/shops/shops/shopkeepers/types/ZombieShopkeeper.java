package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import org.bukkit.Location;


public class ZombieShopkeeper extends StandardShopkeeper {
    public ZombieShopkeeper(Location location, Component name) {
        super(EntityType.ZOMBIE, location, name);
    }

    public ZombieShopkeeper(EntityType<? extends Zombie> type, Location location, Component name) {
        super(type, location, name);
    }

}
