package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDamageEvent extends CustomCancellableEvent {

    @NotNull
    private final LivingEntity damagee;
    private LivingEntity damager;
    private final Entity damagingEntity;
    private final DamageCause cause;
    private double damage;
    private double rawDamage;
    private boolean knockback;
    private long damageDelay = 200;
    private LightningStrike lightning;
    private boolean ignoreArmour;
    private Set<String> reason = new HashSet<>();
    private @NotNull SoundProvider soundProvider = SoundProvider.DEFAULT;

    private boolean doVanillaEvent;

    private long forceDamageDelay = 0;

    /**
     * @param damagee   The entity taking damage
     * @param damager   The entity to take credit for this damage (if it's a living entity)
     * @param damagingEntity The entity dealing causing the damage
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     * @param knockback Whether or not the damage should knockback
     */
    public CustomDamageEvent(@NotNull LivingEntity damagee, LivingEntity damager, Entity damagingEntity, DamageCause cause, double damage, boolean knockback) {
        this.damagee = damagee;
        this.damager = damager;
        this.damagingEntity = damagingEntity;
        this.cause = cause;
        this.damage = damage;
        this.rawDamage = damage;
        this.knockback = knockback;
    }

    /**
     * @param damagee   The entity taking damage
     * @param damager   The entity to take credit for this damage (if it's a living entity)
     * @param damagingEntity The entity dealing causing the damage
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     * @param knockback Whether or not the damage should knockback
     * @param reason    What caused the damage event
     */
    public CustomDamageEvent(LivingEntity damagee, LivingEntity damager, Entity damagingEntity, DamageCause cause, double damage, boolean knockback, String reason) {
        this(damagee, damager, damagingEntity, cause, damage, knockback);
        this.reason.add(reason);
    }

    /**
     * @param damagee   The entity taking damage
     * @param damager   The entity to take credit for this damage (if it's a living entity)
     * @param damagingEntity The entity dealing causing the damage
     * @param lightning The lightning strike dealing damage
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     * @param knockback Whether or not the damage should knockback
     */
    public CustomDamageEvent(LivingEntity damagee, LivingEntity damager, Entity damagingEntity, LightningStrike lightning, DamageCause cause, double damage, boolean knockback) {
        this.damagee = damagee;
        this.damager = damager;
        this.damagingEntity = damagingEntity;
        this.cause = cause;
        this.damage = damage;
        this.rawDamage = damage;
        this.knockback = knockback;
        this.lightning = lightning;
    }

    public Projectile getProjectile() {
        return damagingEntity instanceof Projectile ? (Projectile) damagingEntity : null;
    }

    public void addReason(String reason) {
        this.reason.add(reason);
    }

    public String[] getReason() {
        return reason.toArray(String[]::new);
    }

    public boolean hasReason(String reason) {
        return this.reason.stream().anyMatch(s -> s.equalsIgnoreCase(reason));
    }

    public boolean hasReason() {
        return !reason.isEmpty();
    }

    /**
     * Sets the damage of the event
     *
     * @param dam The amount of damage the living entity should take
     */
    public void setDamage(double dam) {
        this.damage = Math.max(0, dam);
    }

    /**
     * Adds a certain amount of damage to the already existing damage amount;
     *
     * @param dam The amount of damage to add;
     */
    public void addDamage(double dam) {
        this.damage += dam;
    }

    /**
     * Takes a certain amount of damage away from the already existing damage amount
     *
     * @param dam The amount of damage to remove
     */
    public void removeDamage(double dam) {
        this.damage -= dam;
    }

}
