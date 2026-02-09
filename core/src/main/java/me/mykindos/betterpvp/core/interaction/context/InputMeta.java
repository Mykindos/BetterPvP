package me.mykindos.betterpvp.core.interaction.context;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Built-in meta keys for input properties.
 * These are populated by the InteractionListener when specific inputs trigger interactions.
 * <p>
 * <b>Important:</b> All InputMeta keys are <b>execution-scoped</b>, meaning they are cleared
 * at the start of each interaction execution and only valid within the same execution.
 * This prevents values from one interaction (e.g., DAMAGE_DEALT) bleeding into another
 * interaction (e.g., HOLD_RIGHT_CLICK) within the same chain.
 * <p>
 * Use {@link InteractionContext#get(ExecutionKey)} to read these values and
 * {@link InteractionContext#has(ExecutionKey)} to check if they are set.
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
    public static final ExecutionKey<DamageEvent> DAMAGE_EVENT = ExecutionKey.of("input.damage_event");

    /**
     * The entity that was damaged.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final ExecutionKey<LivingEntity> TARGET = ExecutionKey.of("input.target");

    /**
     * The entity that dealt the damage.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final ExecutionKey<LivingEntity> DAMAGER = ExecutionKey.of("input.damager");

    /**
     * The amount of damage dealt.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final ExecutionKey<Double> DAMAGE_AMOUNT = ExecutionKey.ofDouble("input.damage_amount");

    /**
     * The final damage after modifiers.
     * Available for: DAMAGE_DEALT, DAMAGE_TAKEN
     */
    public static final ExecutionKey<Double> FINAL_DAMAGE = ExecutionKey.ofDouble("input.final_damage");

    // ==================== Kill Input Properties ====================
    // Populated when KILL input triggers

    /**
     * The entity that was killed.
     * Available for: KILL
     */
    public static final ExecutionKey<LivingEntity> KILLED_ENTITY = ExecutionKey.of("input.killed_entity");

    /**
     * The player who got the kill.
     * Available for: KILL
     */
    public static final ExecutionKey<Player> KILLER = ExecutionKey.of("input.killer");

    // ==================== Location Input Properties ====================
    // Populated for location-based inputs

    /**
     * The target location for the interaction.
     * Available for: Various inputs that involve targeting a location
     */
    public static final ExecutionKey<Location> TARGET_LOCATION = ExecutionKey.of("input.target_location");

    /**
     * The origin location where the interaction started.
     * Available for: Various inputs
     */
    public static final ExecutionKey<Location> ORIGIN_LOCATION = ExecutionKey.of("input.origin_location");

    // ==================== Sneak Input Properties ====================
    // Populated when SNEAK_START or SNEAK_END inputs trigger

    /**
     * Whether the player is currently sneaking.
     * Available for: SNEAK_START, SNEAK_END
     */
    public static final ExecutionKey<Boolean> IS_SNEAKING = ExecutionKey.ofBoolean("input.is_sneaking");

    // ==================== Projectile Input Properties ====================
    // Populated when PROJECTILE_HIT input triggers

    /**
     * The entity hit by a projectile.
     * Available for: PROJECTILE_HIT
     */
    public static final ExecutionKey<LivingEntity> PROJECTILE_HIT_ENTITY = ExecutionKey.of("input.projectile_hit_entity");

    /**
     * The location where a projectile hit.
     * Available for: PROJECTILE_HIT
     */
    public static final ExecutionKey<Location> PROJECTILE_HIT_LOCATION = ExecutionKey.of("input.projectile_hit_location");

    // ==================== Running Interaction Properties ====================
    // Populated automatically for Running interactions

    /**
     * Set to true only during the first execution of a Running interaction.
     * Use {@code context.has(InputMeta.FIRST_RUN)} to check if this is the first execution.
     * Available for: Running interactions (first tick only)
     */
    public static final ExecutionKey<Boolean> FIRST_RUN = ExecutionKey.ofBoolean("input.first_run");

    /**
     * Set to true only during the last execution of a Running interaction (before timeout).
     * Use {@code context.has(InputMeta.LAST_RUN)} to check if this is the final execution.
     * Available for: Running interactions (last tick only, when next tick would timeout)
     */
    public static final ExecutionKey<Boolean> LAST_RUN = ExecutionKey.ofBoolean("input.last_run");

    // ==================== Free-to-Use Properties ====================

    public static final ExecutionKey<Set<UUID>> DAMAGED_ENTITIES = ExecutionKey.ofSet("damaged_entities");

}
