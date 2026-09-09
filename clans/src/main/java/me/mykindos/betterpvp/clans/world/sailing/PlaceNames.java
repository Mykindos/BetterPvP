package me.mykindos.betterpvp.clans.world.sailing;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Makes up a name for an instance that has no fixed one, so it reads as a place rather than a numbered slot.
 */
public final class PlaceNames {

    private static final String[] PREFIXES = {
            "Ashfall", "Gullwind", "Saltmere", "Driftwood", "Cinderrock", "Thistlebrook",
            "Hollowmere", "Windveil", "Amberlight", "Stormrun", "Foxglove", "Duskharbor"
    };

    private static final String[] SUFFIXES = {
            "Reach", "Cay", "Hollow", "Point", "Isle", "Shoals", "Bluff", "Cove", "Ridge", "Landing"
    };

    private PlaceNames() {
    }

    public static String generate() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return PREFIXES[random.nextInt(PREFIXES.length)] + " " + SUFFIXES[random.nextInt(SUFFIXES.length)];
    }
}
