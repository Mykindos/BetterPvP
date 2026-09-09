package me.mykindos.betterpvp.clans.world.sailing;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import lombok.Getter;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import org.jetbrains.annotations.NotNull;

/**
 * One voyage in progress: the crew on it, the destination, the staging world and the start time.
 * <p>
 * The crew is fixed for the duration, so it is held here rather than looked up again. A captain who disconnects part
 * way through must not end the voyage for everyone else.
 */
@Getter
public class Voyage {

    private final Crew crew;
    private final Landfall destination;
    private final VoyageTiming timing;

    /** The staging world the crew occupies, released when the voyage ends. */
    private final String ocean;

    private final long startedAt;

    public Voyage(@NotNull Crew crew, @NotNull Landfall destination, @NotNull VoyageTiming timing,
                  @NotNull String ocean, long startedAt) {
        this.crew = crew;
        this.destination = destination;
        this.timing = timing;
        this.ocean = ocean;
        this.startedAt = startedAt;
    }

    public long elapsedSeconds(long now) {
        return Math.max(0, (now - startedAt) / 1000L);
    }

    /** Whether this roll arrives at the destination. */
    public boolean arrives(long now, double sample) {
        return timing.arrives(elapsedSeconds(now), sample);
    }
}
