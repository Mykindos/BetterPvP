package me.mykindos.betterpvp.core.interaction.context;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Built-in meta keys for input properties.
 * These are populated by the InteractionListener when specific inputs trigger interactions.
 * <p>
 * For example, when DAMAGE_DEALT triggers an interaction, the listener populates
 * {@link #DAMAGE_EVENT}, {@link #TARGET}, and {@link #DAMAGE_AMOUNT}.
 */
public final class InputMeta {

    private InputMeta() {} // Utility class

    // ==================== Damage Input Properties ====================
    // Populated when DAMAGE_DEALT or DAMAGE_TAKEN inputs trigger

    /**
     * The custom DamageEvent that triggered this interaction.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN (when using custom damage system)
     */
    public static final MetaKey<DamageEvent> DAMAGE_EVENT = MetaKey.of("input.damage_event");

    /**
     * The entity that was damaged.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final MetaKey<LivingEntity> TARGET = MetaKey.of("input.target");

    /**
     * The entity that dealt the damage.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final MetaKey<LivingEntity> DAMAGER = MetaKey.of("input.damager");

    /**
     * The amount of damage dealt.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final MetaKey<Double> DAMAGE_AMOUNT = MetaKey.ofDouble("input.damage_amount");

    /**
     * The final damage after modifiers.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final MetaKey<Double> FINAL_DAMAGE = MetaKey.ofDouble("input.final_damage");

    // ==================== Kill Input Properties ====================
    // Populated when KILL input triggers

    /**
     * The entity that was killed.
     * Available for: KILL
     */
    public static final MetaKey<LivingEntity> KILLED_ENTITY = MetaKey.of("input.killed_entity");

    /**
     * The player who got the kill.
     * Available for: KILL
     */
    public static final MetaKey<Player> KILLER = MetaKey.of("input.killer");

    // ==================== Location Input Properties ====================
    // Populated for location-based inputs

    /**
     * The target location for the interaction.
     * Available for: Various inputs that involve targeting a location
     */
    public static final MetaKey<Location> TARGET_LOCATION = MetaKey.of("input.target_location");

    /**
     * The origin location where the interaction started.
     * Available for: Various inputs
     */
    public static final MetaKey<Location> ORIGIN_LOCATION = MetaKey.of("input.origin_location");

    // ==================== Sneak Input Properties ====================
    // Populated when SNEAK_START or SNEAK_END inputs trigger

    /**
     * Whether the player is currently sneaking.
     * Available for: SNEAK_START, SNEAK_END
     */
    public static final MetaKey<Boolean> IS_SNEAKING = MetaKey.ofBoolean("input.is_sneaking");

    // ==================== Projectile Input Properties ====================
    // Populated when PROJECTILE_HIT input triggers

    /**
     * The entity hit by a projectile.
     * Available for: PROJECTILE_HIT
     */
    public static final MetaKey<LivingEntity> PROJECTILE_HIT_ENTITY = MetaKey.of("input.projectile_hit_entity");

    /**
     * The location where a projectile hit.
     * Available for: PROJECTILE_HIT
     */
    public static final MetaKey<Location> PROJECTILE_HIT_LOCATION = MetaKey.of("input.projectile_hit_location");

    // ==================== Running Interaction Properties ====================
    // Populated automatically for Running interactions

    /**
     * Set to true only during the first execution of a Running interaction.
     * Use {@code context.has(InputMeta.FIRST_RUN)} to check if this is the first execution.
     * Available for: Running interactions (first tick only)
     */
    public static final MetaKey<Boolean> FIRST_RUN = MetaKey.ofBoolean("input.first_run");

    /**
     * Set to true only during the last execution of a Running interaction (before timeout).
     * Use {@code context.has(InputMeta.LAST_RUN)} to check if this is the final execution.
     * Available for: Running interactions (last tick only, when next tick would timeout)
     */
    public static final MetaKey<Boolean> LAST_RUN = MetaKey.ofBoolean("input.last_run");
}
