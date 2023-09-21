package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.bukkit.Location;

public class SkeletonShopkeeper extends StandardShopkeeper {
    public SkeletonShopkeeper(Location location, Component name) {
        super(EntityType.SKELETON, location, name);
    }

    public SkeletonShopkeeper(EntityType<? extends AbstractSkeleton> type, Location location, Component name) {
        super(type, location, name);
    }
}
