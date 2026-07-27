package me.mykindos.betterpvp.core.item.impl.cannon.ammo;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A shot in flight: the entity that carries it, plus everything needed to resolve its impact once the cannon that
 * fired it may already be gone (destroyed, or dematerialized by a chunk unload).
 * <p>
 * Held by {@link me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService} for the duration of the flight and
 * handed back to the owning {@link CannonAmmo} each tick, so ammo types stay stateless singletons.
 */
@Getter
public class CannonProjectile {

    private final @NotNull Entity entity;
    private final @NotNull CannonAmmo ammo;
    private final @Nullable UUID shooter;

    /** Damage scale inherited from the firing cannon's properties, captured at launch. */
    private final double damageMultiplier;

    /** Collision policy resolved at launch: the ammo's default unless the cannon vetoed it. */
    private final boolean entityCollision;
    private final boolean blockCollision;

    /** Whether the firing cannon permits its shots to break terrain. */
    private final boolean breaksBlocks;

    /** Previous tick's position, used for swept collision so fast shots cannot tunnel. */
    @Setter private @NotNull Location lastLocation;

    public CannonProjectile(@NotNull Entity entity, @NotNull CannonAmmo ammo, @Nullable UUID shooter,
                            @NotNull CannonProp cannon) {
        this.entity = entity;
        this.ammo = ammo;
        this.shooter = shooter;
        this.lastLocation = entity.getLocation();
        this.damageMultiplier = cannon.getProperties().getDamageMultiplier();
        this.entityCollision = cannon.getProperties().resolveEntityCollision(ammo.explodesOnEntityHit());
        this.blockCollision = cannon.getProperties().resolveBlockCollision(ammo.explodesOnBlockHit());
        this.breaksBlocks = cannon.getProperties().isBreaksBlocks();
    }

    /** The damage this shot deals {@code distance} blocks from its impact point, after the cannon's multiplier. */
    public double damageAt(double distance) {
        return ammo.damageAt(distance) * damageMultiplier;
    }
}
