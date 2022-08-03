package me.mykindos.betterpvp.core.combat.events;

import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class CustomDamageEvent extends CustomEvent {

    private final LivingEntity damagee;
    private final LivingEntity damager;
    private final Projectile proj;
    private final DamageCause cause;
    private double damage;
    private boolean knockback;
    private long damageDelay = 200;
    private LightningStrike lightning;
    private boolean ignoreArmour;
    private String reason = "";


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
        this.proj = proj;
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
        this.proj = proj;
        this.cause = cause;
        this.damage = damage;
        this.knockback = knockback;
        this.lightning = lightning;

    }

    public void setIgnoreArmour(boolean b) {
        this.ignoreArmour = b;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isIgnoreArmour() {
        return ignoreArmour;
    }

    public long getDamageDelay() {
        return damageDelay;
    }

    public void setDamageDelay(long l) {
        this.damageDelay = l;
    }


    public LivingEntity getDamagee() {
        return damagee;
    }


    public LivingEntity getDamager() {
        return damager;
    }

    public LightningStrike getLightning() {
        return lightning;
    }

    public Projectile getProjectile() {
        return proj;
    }

    public DamageCause getCause() {
        return cause;
    }

    public double getDamage() {
        return damage;
    }

    public boolean getKnockback() {
        return knockback;
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

    /**
     * Decides whether or not a player takes knockback from an attack.
     *
     * @param bool True = Knockback, False = No knockback
     */
    public void setKnockback(boolean bool) {
        this.knockback = bool;
    }


}
