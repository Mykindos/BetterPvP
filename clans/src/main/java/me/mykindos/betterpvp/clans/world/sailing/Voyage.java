package me.mykindos.betterpvp.clans.world.sailing;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import lombok.Getter;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import org.jetbrains.annotations.NotNull;

/**
 * A crew at sea: who is aboard, where they are headed, and how long they have been going.
 * <p>
 * The crew is frozen for the duration, so this holds it rather than re-reading membership — a captain who logs out
 * mid-crossing must not take everyone else's voyage with them.
 */
@Getter
public class Voyage {

    private final Crew crew;
    private final Landfall destination;
    private final VoyageTiming timing;

    /** The private stretch of ocean they are crossing, released when they land. */
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

    /** Whether land is sighted on this roll. */
    public boolean arrives(long now, double sample) {
        return timing.arrives(elapsedSeconds(now), sample);
    }
}
