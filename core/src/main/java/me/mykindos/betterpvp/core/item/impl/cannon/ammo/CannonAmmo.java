package me.mykindos.betterpvp.core.item.impl.cannon.ammo;

import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One kind of thing a cannon can fire. There is exactly one instance per kind ({@code @Singleton}), registered with
 * {@link CannonAmmoRegistry} and keyed by the item that loads it.
 * <p>
 * Ammo owns every <em>projectile</em> trait: damage curve, lifetime, collision policy, impact effect. The cannon
 * owns only the traits that are about the cannon itself (power, size, whether it breaks blocks) and may veto an
 * ammo's collision defaults through {@link me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProperties}.
 *
 * @see ExplosiveShell
 * @see ClusterShell
 */
public interface CannonAmmo {

    /** Stable id, persisted in a cannon's store record so a loaded cannon survives a restart still loaded. */
    @NotNull String id();

    /** The {@code namespace:key} of the item that loads this ammo into a cannon. */
    @NotNull String itemKey();

    /** Shown on the cannon's instruction tag when this ammo is chambered. */
    @NotNull Component displayName();

    /**
     * Spawns the projectile entity and gives it its initial velocity.
     *
     * @param cannon    the firing cannon (for properties and world)
     * @param muzzle    where the shot leaves the barrel
     * @param direction the barrel direction, already scaled by the cannon's power
     * @param shooter   who gets credit for the damage, or {@code null} for an unowned cannon
     */
    @NotNull CannonProjectile launch(@NotNull CannonProp cannon, @NotNull Location muzzle, @NotNull Vector direction,
                                     @Nullable UUID shooter);

    /** Per-tick hook while in flight - trails, homing, spin. Called after collision checks pass. */
    default void tick(@NotNull CannonProjectile projectile) {
    }

    /**
     * The shot reached the end of its flight (hit something or timed out). Implementations detonate, split, ignite -
     * whatever this ammo does. The projectile entity is removed by the caller afterwards if it is still alive.
     */
    void onImpact(@NotNull CannonProjectile projectile, @NotNull Location at);

    /** Damage dealt to something {@code distance} blocks from the impact point, before the cannon's multiplier. */
    default double damageAt(double distance) {
        return 0;
    }

    /** Whether this ammo detonates when it sweeps through a living entity. A cannon may veto this. */
    default boolean explodesOnEntityHit() {
        return true;
    }

    /** Whether this ammo detonates when it touches a solid block. A cannon may veto this. */
    default boolean explodesOnBlockHit() {
        return true;
    }
}
