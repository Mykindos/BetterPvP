package me.mykindos.betterpvp.shops.shopkeepers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class ZombieShopkeeper extends Shopkeeper {
    public ZombieShopkeeper(Level world) {
        super(EntityType.ZOMBIE, world);
    }

    public ZombieShopkeeper(EntityType<? extends Zombie> type, Level world) {
        super(type, world);
    }

}
