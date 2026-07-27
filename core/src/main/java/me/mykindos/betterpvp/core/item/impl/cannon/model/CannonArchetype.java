package me.mykindos.betterpvp.core.item.impl.cannon.model;

import lombok.Builder;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.impl.cannon.firing.CannonFiringMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * A named kind of cannon: the model it wears, how long its fuse and cooldown run, what it will accept as a load, and
 * the {@link CannonFiringMode} that decides what happens when it goes off.
 * <p>
 * Cannon variants are archetypes rather than {@code CannonProp} subclasses - every cannon shares one prop class, and
 * the behavioural difference lives in the firing mode.
 */
@Getter
@Builder
public class CannonArchetype {

    /** Stable id, persisted with every cannon and used as the {@code /cannon spawn} type. */
    private final @NotNull String id;

    /** ModelEngine model id worn by cannons of this kind. */
    @Builder.Default
    private final @NotNull String modelId = "cannon";

    /**
     * How long the fuse burns before the cannon fires itself. A supplier rather than a number so timings stay live
     * across a {@link CannonConfig} reload, including for cannons already standing in the world.
     */
    private final @NotNull DoubleSupplier fuse;

    /** How long after a shot before the cannon may be used again. See {@link #fuse}. */
    private final @NotNull DoubleSupplier cooldown;

    /** What this kind of cannon does when it fires. */
    private final @NotNull CannonFiringMode firingMode;

    /**
     * Ammo ids this cannon accepts, or {@code null} for any registered ammo. Ignored by firing modes that do not
     * consume ammo at all.
     */
    @Builder.Default
    private final @Nullable Set<String> allowedAmmo = null;

    public double getFuseSeconds() {
        return fuse.getAsDouble();
    }

    public double getCooldownSeconds() {
        return cooldown.getAsDouble();
    }

    /** @return whether this archetype will accept a round of {@code ammoId} */
    public boolean accepts(@NotNull String ammoId) {
        return allowedAmmo == null || allowedAmmo.contains(ammoId);
    }
}
