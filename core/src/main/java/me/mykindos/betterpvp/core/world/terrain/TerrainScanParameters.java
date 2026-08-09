package me.mykindos.betterpvp.core.world.terrain;

import lombok.Value;
import org.bukkit.Material;

import java.util.Set;

/**
 * The tunable inputs to a terrain scan: what counts as sea level, how wide a beach runs, how high the mountain starts,
 * and which surface blocks the beach is allowed to chase. Held apart from the scan itself so the same classification
 * can be driven from config, a command, or a test with different numbers.
 */
@Value
public class TerrainScanParameters {

    /** The Y of the sea surface. A column is ocean-eligible when its block at this Y is water. */
    int seaLevel;

    /** The guaranteed shoreline width in blocks: land this close to the ocean is beach regardless of its surface. */
    int beachMinWidth;

    /** The maximum shoreline width: beach material is chased out to here, but no further. */
    int beachMaxWidth;

    /** The Y at or above which connected land grown from a peak seed becomes mountain. */
    int mountainBaseY;

    /** The surface materials the beach chases between {@link #beachMinWidth} and {@link #beachMaxWidth} (e.g. sand, gravel). */
    Set<Material> beachMaterials;
}
