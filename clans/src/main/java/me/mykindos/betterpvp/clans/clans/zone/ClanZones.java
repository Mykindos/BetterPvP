package me.mykindos.betterpvp.clans.clans.zone;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Identity, tag, and priority constants for clans-owned zones, mirroring the role
 * {@link me.mykindos.betterpvp.core.world.zone.Zones} plays in core.
 * <p>
 * Server-owned areas that used to be modelled as admin clans (spawn, shops, Fields) are now Mapper region zones loaded
 * by {@link ClanRegionZoneLoader}. They carry the cross-cutting {@link me.mykindos.betterpvp.core.world.zone.Zones}
 * capability tags ({@code safe}, {@code no_build}) plus, for resource areas, {@link #FIELDS}.
 */
public final class ClanZones {

    private ClanZones() {
    }

    /** Tag marking a zone as clan territory. */
    public static final String TERRITORY = "clan_territory";

    /** Tag marking a server region zone as a Fields resource area (replaces the old {@code "Fields"} admin clan). */
    public static final String FIELDS = "fields";

    /** Resolution priority for clan territory zones. */
    public static final int CLAN_TERRITORY_PRIORITY = 5;

    /** Default resolution priority for server region zones — above clan territory so they win on overlap. */
    public static final int SERVER_REGION_PRIORITY = 50;

    /**
     * @param regionName the Mapper data-point name
     * @return the stable zone {@link Key} for a server region of that name
     */
    public static @NotNull Key regionKey(@NotNull String regionName) {
        return Key.key("clans", "region_" + regionName.toLowerCase().replace(' ', '_'));
    }
}
