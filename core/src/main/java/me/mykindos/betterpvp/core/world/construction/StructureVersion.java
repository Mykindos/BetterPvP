package me.mykindos.betterpvp.core.world.construction;

import lombok.Value;

import java.time.Duration;

/** One step of a structure's version chain: what it looks like, and what taking it to this step costs. */
@Value
public class StructureVersion {

    /** The name the schematic service loads the build under. */
    String schematic;
    ResourceCost cost;
    Duration buildTime;
}
