package me.mykindos.betterpvp.core.world.zone;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.mykindos.betterpvp.core.world.terrain.TerrainMask;
import me.mykindos.betterpvp.core.world.terrain.TerrainType;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link ZoneBounds} that contains a column iff a {@link TerrainMask} classifies it as one {@link TerrainType}. This is
 * how a scanned terrain mask becomes a zone: one bounds per non-interior type (ocean, beach, mountain), each covering
 * the arbitrarily-shaped, non-chunk-aligned footprint the scan produced.
 * <p>
 * Containment is deliberately height-independent — a column belongs to the type at every Y — so protection and
 * claim-blocking hold for the whole column and a swimmer at the surface resolves the same as a diver below. The covered
 * chunks come straight from the mask, so the owning zone files into the spatial index for near-O(1) resolution.
 */
public final class ColumnMaskBounds implements ZoneBounds {

    private final TerrainMask mask;
    private final TerrainType type;
    private final LongSet chunks;

    public ColumnMaskBounds(@NotNull TerrainMask mask, @NotNull TerrainType type) {
        this.mask = mask;
        this.type = type;
        this.chunks = mask.coveredChunks(type);
    }

    @Override
    public boolean contains(@NotNull Location location) {
        return location.getWorld() == mask.getWorld()
                && mask.get(location.getBlockX(), location.getBlockZ()) == type;
    }

    @Override
    public boolean containsColumn(@NotNull World world, int x, int y, int z) {
        return world == mask.getWorld() && mask.get(x, z) == type;
    }

    @Override
    public @Nullable World getWorld() {
        return mask.getWorld();
    }

    @Override
    public @NotNull LongSet coveredChunks() {
        return chunks;
    }
}
