package me.mykindos.betterpvp.core.world.zone;

import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.PolygonRegion;
import dev.brauw.mapper.region.Region;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Free-form {@link ZoneBounds} backed by a Mapper {@link Region}. The region is the source of truth; nothing is
 * persisted. Containment delegates straight to {@link Region#contains(Location)}, and the overlapping chunks are
 * precomputed once so the owning zone can be registered into the spatial index.
 */
public final class RegionBounds implements ZoneBounds {

    private final Region region;
    private final LongSet chunks;

    private RegionBounds(@NotNull Region region) {
        this.region = region;
        this.chunks = computeChunks(region);
    }

    /**
     * @param region the Mapper region to wrap (must already have its world set)
     * @return a bounds delegating to the region
     */
    public static RegionBounds of(@NotNull Region region) {
        return new RegionBounds(region);
    }

    @Override
    public boolean contains(@NotNull Location location) {
        return region.getWorld() != null
                && location.getWorld() == region.getWorld()
                && region.contains(location);
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public @Nullable World getWorld() {
        return region.getWorld();
    }

    @Override
    public @NotNull LongSet coveredChunks() {
        return chunks;
    }

    private static LongSet computeChunks(Region region) {
        final LongSet set = new LongOpenHashSet();
        addRegion(set, region);
        return set;
    }

    private static void addRegion(LongSet set, Region region) {
        switch (region) {
            case CuboidRegion cuboid -> addCuboid(set, cuboid);
            case PolygonRegion polygon -> polygon.getChildren().forEach(child -> addCuboid(set, child));
            // PerspectiveRegion extends PointRegion, so this covers both single-point shapes.
            case PointRegion point -> set.add(Chunk.getChunkKey(point.getLocation()));
            // Unknown region type: leave empty -> the zone is treated as ambient, still correct via contains().
            default -> { }
        }
    }

    private static void addCuboid(LongSet set, CuboidRegion cuboid) {
        final int minChunkX = cuboid.getMin().getBlockX() >> 4;
        final int maxChunkX = cuboid.getMax().getBlockX() >> 4;
        final int minChunkZ = cuboid.getMin().getBlockZ() >> 4;
        final int maxChunkZ = cuboid.getMax().getBlockZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                set.add(Chunk.getChunkKey(chunkX, chunkZ));
            }
        }
    }
}
