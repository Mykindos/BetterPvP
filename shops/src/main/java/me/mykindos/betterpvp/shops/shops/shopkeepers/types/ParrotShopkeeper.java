package me.mykindos.betterpvp.shops.shops.shopkeepers.types;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ParrotShopkeeper extends Parrot implements IShopkeeper {

    @Getter
    private final CraftEntity entity;

    private String shopkeeperName;

    public ParrotShopkeeper(Location location, Component name) {
        this(EntityType.PARROT, location, name);
        this.shopkeeperName = PlainTextComponentSerializer.plainText().serialize(name);
    }

    public ParrotShopkeeper(EntityType<? extends Parrot> type, Location location, Component name) {
        super(type, ((CraftWorld) location.getWorld()).getHandle());

        goalSelector.removeAllGoals(Objects::nonNull);
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 16.0F));

        entity = spawn(location);

        entity.customName(name);
        entity.setCustomNameVisible(true);

        if(entity instanceof LivingEntity livingEntity) {
            livingEntity.setRemoveWhenFarAway(false);
            livingEntity.setCanPickupItems(false);
        }



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

    // Don't animate damage
    @Override
    public void animateHurt(float yaw) {}

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
    public SoundEvent getAmbientSound() {
        return null;
    }

    // Remove hurt sound
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return null;
    }

    @Override
    public void playHurtSound(@NotNull DamageSource damageSource) {}

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
    public void setSecondsOnFire(int i, boolean callEvent) {
        clearFire();
    }

    // Prevent flying
    @Override
    public boolean isFlying() {
        return false;
    }

    @Override
    public boolean isPartyParrot() {
        return true;
    }

    @Override
    public boolean onGround() {
        return true;
    }

    @Override
    public boolean isSilent(){
        return true;
    }

    @Override
    public void tame(@NotNull Player player) {

    }

    public CraftEntity spawn(Location loc) {
        this.absMoveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return getBukkitEntity();
    }

    @Override
    public String getShopkeeperName() {
        return shopkeeperName;
    }

}
