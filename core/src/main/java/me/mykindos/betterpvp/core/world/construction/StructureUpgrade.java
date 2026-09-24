package me.mykindos.betterpvp.core.world.construction;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * One of the upgrades a structure offers at a stage. A structure takes at most one upgrade from each stage, and a
 * stage it has reached but not picked from can still be picked later.
 */
@Value
public class StructureUpgrade {

    String id;
    /** The stage that offers it, 0 being the first. */
    int stage;
    ResourceCost cost;
    /** How long fitting it takes. Zero fits it at once. */
    Duration time;
    /** How much Workforce a crew needs to fit it. 0 needs no crew. */
    int workforce;
    /**
     * The build added to the structure once it has the upgrade, placed on the point named {@code upgrade:<id>} in the
     * structure's build. Null when the upgrade adds no blocks.
     */
    @Nullable String piece;

    /** The name of the point in a structure's build where this upgrade's piece goes. */
    public String point() {
        return "upgrade:" + id;
    }
}
