package me.mykindos.betterpvp.core.world.travel;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A player who has committed to a {@link Destination} and is being held in place until they reach it, tracked by
 * {@link DepartureController} until they either arrive or are interrupted.
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
