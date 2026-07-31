package me.mykindos.betterpvp.clans.world.island;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates a small place-name for an {@link IslandOffer} so it reads as a destination rather than a numbered slot.
 */
final class IslandPlaceNames {

    private static final String[] PREFIXES = {
            "Ashfall", "Gullwind", "Saltmere", "Driftwood", "Cinderrock", "Thistlebrook",
            "Hollowmere", "Windveil", "Amberlight", "Stormrun", "Foxglove", "Duskharbor"
    };

    private static final String[] SUFFIXES = {
            "Reach", "Cay", "Hollow", "Point", "Isle", "Shoals", "Bluff", "Cove", "Ridge", "Landing"
    };

    private IslandPlaceNames() {
    }

    static String generate() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return PREFIXES[random.nextInt(PREFIXES.length)] + " " + SUFFIXES[random.nextInt(SUFFIXES.length)];
    }
}
