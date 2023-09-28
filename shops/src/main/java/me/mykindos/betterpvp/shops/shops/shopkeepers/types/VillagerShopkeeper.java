package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.ServerLevelAccessor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class VillagerShopkeeper extends StandardShopkeeper {
    public VillagerShopkeeper(Location location, Component name) {
        super(EntityType.VILLAGER, location, name);
    }

    public VillagerShopkeeper(EntityType<? extends AbstractVillager> type, Location location, Component name) {
        super(type, location, name);
    }

    /*
     * This should force all spawned villagers to be default, not influenced by biome
     * I don't know if this has any effect anymore since I am not calling the NMS constructor for villagers
     */
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
    }


}
