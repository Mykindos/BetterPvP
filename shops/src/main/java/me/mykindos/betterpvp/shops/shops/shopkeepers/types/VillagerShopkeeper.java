package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import org.bukkit.Location;

public class VillagerShopkeeper extends StandardShopkeeper {
    public VillagerShopkeeper(Location location, Component name) {
        super(EntityType.VILLAGER, location, name);
    }

    public VillagerShopkeeper(EntityType<? extends AbstractVillager> type, Location location, Component name) {
        super(type, location, name);
    }


}
