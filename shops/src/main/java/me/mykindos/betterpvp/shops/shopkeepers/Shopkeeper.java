package me.mykindos.betterpvp.shops.shopkeepers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class Shopkeeper extends Mob {

    public Shopkeeper(EntityType<? extends Mob> type, Level world) {
        super(type, world);

        goalSelector.removeAllGoals(Objects::nonNull);
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16.0F));
    }

    // Prevent NPC movement
    @Override
    public void setDeltaMovement(@NotNull Vec3 vec3d) {}

    // Prevent thunder from damaging NPC
    @Override
    public void thunderHit(@NotNull ServerLevel worldServer, @NotNull LightningBolt entityLightning) {}

    // Prevent shopkeeper from being pushed
    @Override
    public boolean isPushable() {
        return false;
    }

    // Prevent knockback
    @Override
    public void knockback(double d0, double d1, double d2) {}

    // Prevent damage
    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float f) {
        return false;
    }

    // Make unattackable
    @Override
    public boolean attackable() {
        return false;
    }

    // Prevent teleport
    @Override
    public boolean randomTeleport(double d0, double d1, double d2, boolean flag) {
        return false;
    }

    // Remove ambient sound
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    // Remove hurt sound
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return null;
    }

    // Remove death sound
    @Override
    public SoundEvent getDeathSound() {
        return null;
    }

    // Dont give default equipment
    @Override
    protected void populateDefaultEquipmentSlots(@NotNull RandomSource random, @NotNull DifficultyInstance localDifficulty) {}

    // Dont allow shopkeepers to be set on fire
    @Override
    public void setSecondsOnFire(int i, boolean callEvent) {}

    public CraftEntity spawn(Location loc) {
        this.absMoveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return getBukkitEntity();
    }

}
