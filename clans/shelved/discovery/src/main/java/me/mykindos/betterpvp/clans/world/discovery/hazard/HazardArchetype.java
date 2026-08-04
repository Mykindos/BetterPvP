package me.mykindos.betterpvp.clans.world.discovery.hazard;

import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import org.jetbrains.annotations.NotNull;

/**
 * The code behind one kind of hazard: what it is called, how much sea it takes up, and how to put a live one out there.
 * <p>
 * Same bargain the props make. The archetype is the code and there is exactly one of it; a {@link Hazard} is the
 * instance and there is one per thing currently floating on an expedition's stretch of water. Adding a kind of hazard
 * is therefore a class and a line in {@link HazardRegistry}, and nothing that spawns, draws or collides them has to
 * learn about it.
 */
public interface HazardArchetype {

    /** The name this archetype is claimed under. */
    @NotNull String key();

    /**
     * Blocks of clearance around the hazard's own point that count as running into it. Measured on the surface only —
     * everything at sea sits at water level.
     */
    double radius();

    /** Puts a live one at a fixed spot on the plane. The spot never moves; the ship sails past it. */
    @NotNull Hazard create(@NotNull OceanPoint at);
}
