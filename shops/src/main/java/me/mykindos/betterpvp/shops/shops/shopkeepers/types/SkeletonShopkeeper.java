package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.bukkit.Location;

public class SkeletonShopkeeper extends StandardShopkeeper {
    public SkeletonShopkeeper(Location location, String name) {
        super(EntityType.SKELETON, location, name);
    }

    public SkeletonShopkeeper(EntityType<? extends AbstractSkeleton> type, Location location, String name) {
        super(type, location, name);
    }
}
