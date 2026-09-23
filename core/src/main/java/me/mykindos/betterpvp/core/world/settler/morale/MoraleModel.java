package me.mykindos.betterpvp.core.world.settler.morale;

import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/** How a site's settlers feel, and when feeling bad makes them leave. The site that owns them decides all of it. */
public interface MoraleModel {

    /** {@code settler}'s morale at {@code now}, from -100 to 100 where 0 is neutral. */
    int morale(@NotNull SiteKey site, @NotNull Settler settler, @NotNull Roster roster, long now);

    /** Morale below which a settler starts thinking of leaving. */
    int leaveBelow();

    /** How long a settler stays that unhappy before it leaves. */
    @NotNull Duration leaveAfter();

    /** Whether {@code settler} would ever leave over low morale. */
    default boolean mayLeave(@NotNull Settler settler) {
        return true;
    }
}
