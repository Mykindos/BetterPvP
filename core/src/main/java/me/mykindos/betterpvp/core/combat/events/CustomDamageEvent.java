package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDamageEvent extends CustomCancellableEvent {

    @NotNull
    private final LivingEntity damagee;
    private final LivingEntity damager;
    private final Projectile projectile;
    private final DamageCause cause;
    private double damage;
    private double rawDamage;
    private boolean knockback;
    private long damageDelay = 200;
    private LightningStrike lightning;
    private boolean ignoreArmour;
    private String reason = "";

    private boolean doVanillaEvent;


    /**
     * @param damagee   The entity taking damage
     * @param damager   The entity dealing damage (if its a living entity)
     * @param proj      The projectile dealing damage (if its a projectile)
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     * @param knockback Whether or not the damage should knockback
     */
    public CustomDamageEvent(LivingEntity damagee, LivingEntity damager, Projectile proj, DamageCause cause, double damage, boolean knockback) {
        this.damagee = damagee;
        this.damager = damager;
        this.projectile = proj;
        this.cause = cause;
        this.damage = damage;
        this.knockback = knockback;

    }

    /**
     * @param damagee   The entity taking damage
     * @param damager   The entity dealing damage (if its a living entity)
     * @param proj      The projectile dealing damage (if its a projectile)
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     * @param knockback Whether or not the damage should knockback
     * @param reason    What caused the damage event
     */
    public CustomDamageEvent(LivingEntity damagee, LivingEntity damager, Projectile proj, DamageCause cause, double damage, boolean knockback, String reason) {
        this(damagee, damager, proj, cause, damage, knockback);
        this.reason = reason;
    }

    /**
     * @param damagee   The entity taking damage
     * @param damager   The entity dealing damage (if its a living entity)
     * @param proj      The projectile dealing damage (if its a projectile)
     * @param lightning The lightning strike dealing damage
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     * @param knockback Whether or not the damage should knockback
     */
    public CustomDamageEvent(LivingEntity damagee, LivingEntity damager, Projectile proj, LightningStrike lightning, DamageCause cause, double damage, boolean knockback) {
        this.damagee = damagee;
        this.damager = damager;
        this.projectile = proj;
        this.cause = cause;
        this.damage = damage;
        this.knockback = knockback;
        this.lightning = lightning;

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
