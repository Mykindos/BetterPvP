package me.mykindos.betterpvp.clans.world.content;

import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.validation.RegionValidator;
import dev.brauw.mapper.validation.ValidationIssue;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared checks for content authored as Mapper data-points.
 * <p>
 * Everything here catches a mistake that is otherwise <em>silent</em>: content authored with the wrong wand is filtered
 * out by type before its tags are read, and a duplicate id makes one of its holders permanently unreachable. Both look
 * identical in-world - the thing is simply not there - and both surface hours later in a log nobody is reading. Caught
 * here, they surface while the builder is still standing next to the marker.
 */
public abstract class DataPointValidator implements RegionValidator {

    /** All regions authored under one data-point name. */
    protected List<Region> named(@NotNull List<Region> regions, @NotNull String name) {
        return regions.stream().filter(region -> name.equalsIgnoreCase(region.getName())).toList();
    }

    /**
     * Reports data-points authored with the wrong region type. Reported once per offender rather than once per name,
     * since a builder who got one wrong usually got exactly one wrong.
     */
    protected void requireType(@NotNull List<ValidationIssue> issues, @NotNull List<Region> regions,
                               @NotNull Class<? extends Region> required, @NotNull String description) {
        for (Region region : regions) {
            if (!required.isInstance(region)) {
                issues.add(ValidationIssue.error(region,
                        "Must be " + description + ", but was authored as a "
                                + region.getType().name().toLowerCase(Locale.ROOT)));
            }
        }
    }

    /**
     * Indexes data-points by their {@code id:} tag, reporting any two that claim the same one.
     * <p>
     * A repeated id is an error rather than a warning because references resolve to the first holder: the second is not
     * merely ambiguous, it is unreachable, and which of the two wins depends on file order.
     *
     * @param what what one of these is called, for the message ("resident", "route", ...)
     */
    protected Map<String, Region> collectIds(@NotNull List<ValidationIssue> issues, @NotNull List<Region> regions,
                                             @NotNull String what) {
        final Map<String, Region> byId = new HashMap<>();
        for (Region region : regions) {
            final String id = tags(region).getString("id", "").trim();
            if (id.isEmpty()) {
                continue;
            }

            final Region clash = byId.putIfAbsent(id.toLowerCase(Locale.ROOT), region);
            if (clash != null) {
                issues.add(ValidationIssue.error(region,
                        "Another " + what + " already uses id '" + id + "' - only one of them can be referenced"));
            }
        }
        return byId;
    }

    protected RegionTags tags(@NotNull Region region) {
        return RegionTags.of(region);
    }
}
