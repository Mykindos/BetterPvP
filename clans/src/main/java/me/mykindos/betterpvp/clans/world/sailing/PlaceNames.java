package me.mykindos.betterpvp.clans.world.sailing;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates a small place-name for an uncharted island so it reads as a destination rather than a numbered slot.
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
