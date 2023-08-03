package me.mykindos.betterpvp.shops.shopkeepers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.Level;

public class SkeletonShopkeeper extends Shopkeeper {
    public SkeletonShopkeeper(Level world) {
        super(EntityType.SKELETON, world);
    }

    public SkeletonShopkeeper(EntityType<? extends AbstractSkeleton> type, Level world) {
        super(type, world);
    }
}
