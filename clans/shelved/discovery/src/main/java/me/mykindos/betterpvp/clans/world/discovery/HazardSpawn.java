package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;

/** Where a hazard is to appear, as a bearing off the bow and a range — the placement, not the hazard itself. */
@Value
public class HazardSpawn {

    /** Degrees off the bow, {@code 0} dead ahead and {@code +90} abeam to starboard. */
    double bearing;

    /** Blocks from the ship at the moment of spawning. */
    double distance;
}
