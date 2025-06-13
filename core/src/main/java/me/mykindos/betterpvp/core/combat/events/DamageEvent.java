package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.damage.DamageModifiers;
import me.mykindos.betterpvp.core.combat.damage.ModifierType;
import me.mykindos.betterpvp.core.combat.damage.ModifierValue;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class DamageEvent extends CustomCancellableEvent {

    @NotNull
    protected final Entity damagee;
    protected LivingEntity damager;
    protected final DamageSource damageSource;
    protected final EntityDamageEvent.DamageCause cause;
    protected double damage;
    protected double rawDamage;
    protected LightningStrike lightning;
    protected Set<String> reason = new HashSet<>();
    private long damageDelay = 400;
    private @NotNull SoundProvider soundProvider = SoundProvider.DEFAULT;
    private boolean doVanillaEvent;
    private long forceDamageDelay = 0;
    private final DamageModifiers damageModifiers = new DamageModifiers();

    /**
     * @param damagee   The entity taking damage
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     */
    public DamageEvent(@NotNull Entity damagee, DamageSource source, EntityDamageEvent.DamageCause cause, double damage) {
        super(false);
        this.damagee = damagee;
        this.damageSource = source;
        this.cause = cause;
        this.damage = damage;
        this.rawDamage = damage;
        this.damager = (LivingEntity) (source.isIndirect() ? source.getCausingEntity() : source.getDirectEntity());
    }

    /**
     * @param damagee   The entity taking damage
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     * @param reason    What caused the damage event
     */
    public DamageEvent(Entity damagee, DamageSource source, EntityDamageEvent.DamageCause cause, double damage, String reason) {
        this(damagee, source, cause, damage);
        this.reason.add(reason);
    }

    /**
     * @param damagee   The entity taking damage
     * @param lightning The lightning strike dealing damage
     * @param cause     The cause of damage, e.g. fall damage
     * @param damage    The amount of damage to be dealt
     */
    public DamageEvent(Entity damagee, DamageSource source, LightningStrike lightning, EntityDamageEvent.DamageCause cause, double damage) {
        this(damagee, source, cause, damage);
        this.lightning = lightning;
    }

    public Entity getDamagingEntity() {
        return damageSource.getDirectEntity();
    }

    public Projectile getProjectile() {
        return damageSource.isIndirect() && damageSource.getDirectEntity() instanceof Projectile projectile ? projectile : null;
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

    /**
     * Gets the damage modifiers for this event.
     * Listeners can use this to add modifiers that will be processed at the end of the event.
     *
     * @return The damage modifiers
     */
    public DamageModifiers getDamageModifiers() {
        return damageModifiers;
    }

    /**
     * Processes all damage modifiers and updates the damage value.
     * This should be called at the end of the event handling.
     */
    public void processDamageModifiers() {
        // Process damage modifiers
        this.damage = damageModifiers.applyModifiers(ModifierType.DAMAGE, this.damage);

        // Process damage delay modifiers
        this.damageDelay = (long) damageModifiers.applyModifiers(ModifierType.DAMAGE_DELAY, this.damageDelay);
    }

}
