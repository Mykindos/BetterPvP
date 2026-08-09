package me.mykindos.betterpvp.clans.world.terrain;

import me.mykindos.betterpvp.core.world.terrain.TerrainType;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Identity and tag constants for the zones a terrain scan produces, mirroring the role
 * {@link me.mykindos.betterpvp.clans.clans.zone.ClanZones ClanZones} plays for clan-owned areas. Not tied to any one
 * world — every scanned world reuses these.
 */
public final class TerrainZones {

    private TerrainZones() {
    }

    /** The file, in a world's folder, holding that world's saved {@link me.mykindos.betterpvp.core.world.terrain.TerrainMask}. */
    public static final String MASK_FILE_NAME = "terrain-mask.dat";

    /** Tag marking a zone as open ocean; the ocean-damage tick keys off it. */
    public static final String OCEAN = "ocean";

    /**
     * @param world the world the zone lives in
     * @param type  the terrain type the zone represents
     * @return a stable, per-world zone {@link Key} (so the same content on two worlds never collides)
     */
    public static @NotNull Key key(@NotNull World world, @NotNull TerrainType type) {
        final String value = ("terrain_" + type.name() + "_" + world.getName())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.\\-]", "_");
        return Key.key("clans", value);
    }
}
