package me.mykindos.betterpvp.core.combat.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class CustomDamageEvent extends DamageEvent {

    private static DamageSource getSource(Entity damager, Entity damagingEntity) {
        if (damagingEntity != null) {
            return DamageSource.builder(DamageType.GENERIC)
                    .withCausingEntity(damager)
                    .withDirectEntity(damagingEntity)
                    .withDamageLocation(damager.getLocation())
                    .build();
        } else {
            return DamageSource.builder(DamageType.GENERIC)
                    .withDirectEntity(damager)
                    .withCausingEntity(damager)
                    .withDamageLocation(damager.getLocation())
                    .build();
        }
    }

    private boolean ignoreArmour;
    protected boolean knockback;
    private boolean doDurability = true;
    private boolean hurtAnimation = true;

    public CustomDamageEvent(@NotNull LivingEntity damagee, LivingEntity damager, Entity damagingEntity, EntityDamageEvent.DamageCause cause, double damage, boolean knockback) {
        super(damagee, getSource(damager, damagingEntity), cause, damage);
        this.knockback = knockback;
    }

    public CustomDamageEvent(LivingEntity damagee, LivingEntity damager, Entity damagingEntity, EntityDamageEvent.DamageCause cause, double damage, boolean knockback, String reason) {
        super(damagee, getSource(damager, damagingEntity), cause, damage, reason);
        this.knockback = knockback;
    }

    public CustomDamageEvent(LivingEntity damagee, DamageSource source, EntityDamageEvent.DamageCause cause, double damage, boolean knockback) {
        super(damagee, source, cause, damage);
        this.knockback = knockback;
    }

    @Override
    public @NotNull LivingEntity getDamagee() {
        return (LivingEntity) super.getDamagee();
    }
}
