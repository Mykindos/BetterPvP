package me.mykindos.betterpvp.clans.world.discovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Decides whether a crew coming ashore has somebody to fight for the island.
 * <p>
 * Two crews contest an island when they picked the same template, are within a hand of each other in numbers, and
 * arrived close enough together to still be there. Nothing here weighs how dangerous either crew is: an island is worth
 * the same to both of them, and a crew that sails into a stronger one has made a choice rather than been matched into a
 * loss.
 * <p>
 * The decision reads nothing but the records, so it can be exercised without a world.
 */
@Singleton
public class ContestedLandingPolicy {

    /** How far apart two crews may be in numbers and still be put on the same island. */
    private static final int CREW_SIZE_TOLERANCE = 1;

    /** How long a landing stays contestable. */
    @Inject
    @Config(path = "clans.discovery.contest-window", defaultValue = "300000")
    @Getter
    private long windowMillis;

    private final List<ContestedLanding> recent = new CopyOnWriteArrayList<>();

    public ContestedLandingPolicy() {
    }

    ContestedLandingPolicy(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /** Notes a crew as being ashore, and drops anything that has since gone cold. */
    public void record(@NotNull String templateKey, int crewSize, @NotNull UUID instanceId,
                       @NotNull String arrivalPointName, long now) {
        expire(now);
        recent.add(new ContestedLanding(templateKey, crewSize, instanceId, arrivalPointName, now));
    }

    /**
     * The landing a crew of this size would be sailing into, if there is one.
     *
     * @param templateKey the island they are steering for
     * @param crewSize    how many of them are actually coming ashore
     */
    public @NotNull Optional<ContestedLanding> contestFor(@NotNull String templateKey, int crewSize, long now) {
        expire(now);
        return recent.stream()
                .filter(landing -> landing.getTemplateKey().equals(templateKey))
                .filter(landing -> Math.abs(landing.getCrewSize() - crewSize) <= CREW_SIZE_TOLERANCE)
                .findFirst();
    }

    /** How many landings are still contestable. */
    public int size() {
        return recent.size();
    }

    /** Records are dropped as they age out rather than kept, so a long-running server does not accumulate a history. */
    private void expire(long now) {
        recent.removeIf(landing -> now - landing.getAt() > windowMillis);
    }
}
