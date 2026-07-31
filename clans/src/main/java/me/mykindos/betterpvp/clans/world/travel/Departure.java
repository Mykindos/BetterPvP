package me.mykindos.betterpvp.clans.world.travel;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * An in-flight voyage: a traveller mid-departure toward a {@link Destination}, tracked by {@link DepartureController}
 * from the moment they commit until they either arrive or are interrupted.
 */
@Getter
public class Departure {

    private final Player traveller;
    private final Destination destination;
    private final Location origin;
    private final long startTime;
    private final long durationMillis;
    private final Runnable onArrive;

    public Departure(@NotNull Player traveller, @NotNull Destination destination, @NotNull Runnable onArrive, long durationMillis) {
        this.traveller = traveller;
        this.destination = destination;
        this.origin = traveller.getLocation().clone();
        this.startTime = System.currentTimeMillis();
        this.durationMillis = durationMillis;
        this.onArrive = onArrive;
    }

    public long remainingMillis() {
        return Math.max(0, startTime + durationMillis - System.currentTimeMillis());
    }

    public boolean isElapsed() {
        return remainingMillis() <= 0;
    }
}
