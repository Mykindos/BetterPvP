package me.mykindos.betterpvp.hub.feature.zone;

import net.kyori.adventure.key.Key;

/**
 * Identities of the hub's zones, used to compare against the player's current zone via the core
 * {@link me.mykindos.betterpvp.core.world.zone.ZoneManager}.
 */
public final class HubZones {

    private HubZones() {
    }

    /** The combat arena. Registered as an indexed region zone. */
    public static final Key FFA = Key.key("betterpvp", "hub_ffa");

    /** The common area; the hub world's default zone (everyone is here unless in another zone). */
    public static final Key COMMON = Key.key("betterpvp", "hub_common");
}
