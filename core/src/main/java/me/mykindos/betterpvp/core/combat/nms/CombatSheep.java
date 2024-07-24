package me.mykindos.betterpvp.core.combat.nms;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Sheep;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CombatSheep extends Sheep {

    private final Location location;

    public CombatSheep(Location location) {
        super(EntityType.SHEEP, ((CraftWorld) location.getWorld()).getHandle());
        this.location = location;

        this.goalSelector.removeAllGoals(Objects::nonNull);
    }

    // Prevent thunder from damaging NPC
    @Override
    public void thunderHit(@NotNull ServerLevel worldServer, @NotNull LightningBolt entityLightning) {
    }

    // Prevent from being pushed
    @Override
    public boolean isPushable() {
        return false;
    }

    // Prevent knockback
    @Override
    public void knockback(double d0, double d1, double d2) {
    }

    // Prevent damage
    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float f) {
        return false;
    }

    // Don't animate damage
    @Override
    public void animateHurt(float yaw) {
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

    @Override
    public void playHurtSound(@NotNull DamageSource damageSource) {
    }

    // Remove death sound
    @Override
    public SoundEvent getDeathSound() {
        return null;
    }

    // Dont give default equipment
    @Override
    protected void populateDefaultEquipmentSlots(@NotNull RandomSource random, @NotNull DifficultyInstance localDifficulty) {
    }

    public CraftEntity spawn() {
        this.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return getBukkitEntity();
    }
}
