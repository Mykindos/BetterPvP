package me.mykindos.betterpvp.clans.world.discovery.hazard;

import lombok.Getter;
import me.mykindos.betterpvp.clans.world.discovery.Expedition;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * One thing currently floating on an expedition's sea.
 * <p>
 * A hazard owns a fixed {@link OceanPoint} and nothing else about where it is: the ship moves, the point does not, and
 * where it should be drawn this tick is worked out by the field and handed to {@link #render(Location)}. Everything a
 * hazard puts into the world is its own to take back out again in {@link #remove()}, which is called both when it
 * drifts out of range and when the expedition ends.
 */
@Getter
public abstract class Hazard {

    private final HazardArchetype archetype;

    /** Where it sits on the virtual plane. Fixed for the hazard's whole life. */
    private final OceanPoint point;

    protected Hazard(@NotNull HazardArchetype archetype, @NotNull OceanPoint point) {
        this.archetype = archetype;
        this.point = point;
    }

    /**
     * Draws it where the plane currently puts it, called every tick it is within range.
     *
     * @param at the projected world position, on the water beside the moored hull
     */
    public abstract void render(@NotNull Location at);

    /** What running into it costs the crew. Fires once: the field drops the hazard before calling this. */
    public abstract void onCollide(@NotNull Expedition expedition);

    /** Takes anything this hazard put in the world back out of it. */
    public abstract void remove();
}
