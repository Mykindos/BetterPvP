package me.mykindos.betterpvp.clans.world.discovery.hazard;

import lombok.Getter;
import me.mykindos.betterpvp.clans.world.discovery.OceanOffset;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * What is currently floating on one expedition's sea, and whatever has hold of its wheel.
 * <p>
 * The two ways off the field are the only ones there are: drifting out of range, and being hit. Both take the hazard
 * out and call {@link Hazard#remove()}, so nothing entity-backed can be left behind by either.
 * <p>
 * The projection is passed in rather than looked up, which keeps every decision here — what to cull, what to draw,
 * what was struck — answerable without a world.
 */
public class HazardField {

    private final List<Hazard> hazards = new ArrayList<>();

    @Getter
    private final SteeringOverride override = new SteeringOverride();

    public void add(@NotNull Hazard hazard) {
        hazards.add(hazard);
    }

    /** A snapshot of what is out there. */
    public @NotNull List<Hazard> live() {
        return List.copyOf(hazards);
    }

    public int size() {
        return hazards.size();
    }

    /**
     * Advances everything on the field by one tick: culls what has drifted away, draws the rest, and reports the first
     * thing the hull has run into.
     * <p>
     * A struck hazard leaves the field before it is handed back, so the collision cannot fire again on the next tick
     * while the ship is still sitting on top of where it was.
     *
     * @param project       where a fixed point on the plane lies relative to the ship right now
     * @param render        where a ship-local offset falls in the world
     * @param hull          the moored hull, tested in the horizontal only
     * @param despawnRadius how far a hazard may drift before it is taken off the sea
     * @return the hazard that was hit, if any
     */
    public @NotNull Optional<Hazard> sweep(@NotNull Function<OceanPoint, OceanOffset> project,
                                           @NotNull Function<OceanOffset, Location> render,
                                           @NotNull BoundingBox hull, double despawnRadius) {
        Hazard struck = null;

        final Iterator<Hazard> iterator = hazards.iterator();
        while (iterator.hasNext()) {
            final Hazard hazard = iterator.next();
            final OceanOffset offset = project.apply(hazard.getPoint());

            if (HazardGeometry.beyond(offset.distance(), despawnRadius)) {
                iterator.remove();
                hazard.remove();
                continue;
            }

            final Location at = render.apply(offset);
            hazard.render(at);

            // Only the first strike is paid for; the rest keep drifting and get their own tick.
            if (struck == null
                    && HazardGeometry.strikes(hull, at.getX(), at.getZ(), hazard.getArchetype().radius())) {
                iterator.remove();
                hazard.remove();
                struck = hazard;
            }
        }
        return Optional.ofNullable(struck);
    }

    /** Takes every hazard out of the world and hands the wheel back. */
    public void clear() {
        hazards.forEach(Hazard::remove);
        hazards.clear();
        override.release();
    }
}
