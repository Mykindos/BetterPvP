package me.mykindos.betterpvp.core.combat.nms;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

public class CombatSheep extends net.minecraft.world.entity.animal.sheep.Sheep {

    private final Location location;

    public CombatSheep(Location location) {
        super(EntityTypes.SHEEP, ((CraftWorld) location.getWorld()).getHandle());
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
    public void knockback(double power, double xd, double zd, @NotNull DamageSource source, float damage, boolean comesFromEffect,
                          Entity attacker, @NotNull EntityKnockbackEvent.Cause eventCause) {
    }

    // Prevent damage
    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource damageSource, float f) {
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
    public @NotNull RandomTeleportResult randomTeleport(double xx, double yy, double zz, boolean showParticles,
                                                         @NotNull Predicate<BlockState> isInvalidPosition,
                                                         @NotNull PlayerTeleportEvent.TeleportCause cause) {
        return RandomTeleportResult.FAILURE;
    }

    // Remove ambient sound
    @Override
    public SoundEvent getAmbientSound() {
        return null;
    }

    // Remove hurt sound
    @Override
    public SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
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
        this.setPos(location.getX(), location.getY(), location.getZ());
        this.setRot(location.getYaw(), location.getPitch());
        this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return getBukkitEntity();
    }
}
