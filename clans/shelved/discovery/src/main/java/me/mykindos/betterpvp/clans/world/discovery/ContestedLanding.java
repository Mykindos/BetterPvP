package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A crew that has just gone ashore, kept only long enough for another crew to sail into them.
 * <p>
 * The arrival point is part of the record because a contesting crew is put down somewhere else on the same island where
 * the shore allows it — landing on top of the crew already there decides the fight before either of them has seen the
 * other.
 */
@Value
public class ContestedLanding {

    @NotNull String templateKey;

    int crewSize;

    /** The island they hold, which is where a matching crew is delivered. */
    @NotNull UUID instanceId;

    /** The landing they came ashore at, or empty when they were put down somewhere with no marker of its own. */
    @NotNull String arrivalPointName;

    long at;
}
