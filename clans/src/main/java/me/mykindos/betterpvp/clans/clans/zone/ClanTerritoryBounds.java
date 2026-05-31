package me.mykindos.betterpvp.clans.clans.zone;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.ZoneBounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link ZoneBounds} for a clan's territory. Containment delegates to the clan's live chunk ownership, so the bounds
 * stay correct as the clan claims and unclaims land without rebuilding the zone.
 * <p>
 * Reports no covered chunks: clan territory is resolved through {@link ClanZoneProvider} (which reads the chunk PDC, the
 * runtime source of truth), not the static spatial index, so it is never indexed or treated as ambient.
 */
public final class ClanTerritoryBounds implements ZoneBounds {

    private final Clan clan;

    public ClanTerritoryBounds(@NotNull Clan clan) {
        this.clan = clan;
    }

    @Override
    public boolean contains(@NotNull Location location) {
        return clan.isChunkOwnedByClan(UtilWorld.chunkToFile(location.getChunk()));
    }

    @Override
    public @Nullable World getWorld() {
        return Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
    }

    @Override
    public @NotNull LongSet coveredChunks() {
        return LongSets.EMPTY_SET;
    }
}
