package me.mykindos.betterpvp.core.world.construction;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Duration;

/** One step of a structure's stage chain: what it looks like, and what taking it to this step costs. */
@Value
@AllArgsConstructor
public class StructureStage {

    /** The name the schematic service loads the build under. */
    String schematic;
    ResourceCost cost;
    Duration buildTime;
    /** How much Workforce a crew needs for a job to take the structure to this stage. 0 needs no crew. */
    int workforce;

    public StructureStage(String schematic, ResourceCost cost, Duration buildTime) {
        this(schematic, cost, buildTime, 0);
    }
}
