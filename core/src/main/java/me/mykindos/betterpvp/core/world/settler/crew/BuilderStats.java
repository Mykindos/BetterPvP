package me.mykindos.betterpvp.core.world.settler.crew;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/** What one Builder brings to one job, with its trade, rarity and traits already worked in. */
@Value
public class BuilderStats {

    int workforce;
    /** How many times faster than a job's listed time it would run the job alone. */
    double speed;
    /** The share of its speed it adds when it is not the fastest on the crew. */
    double efficiency;
    /** Its trade, or null for none. */
    @Nullable String trade;
    /** The trades it works best beside. */
    @NotNull Set<String> compatible;
}
