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
    /**
     * The XZ footprint as {@code minX, minZ, maxX, maxZ} quads, or {@code null} for a shape whose footprint we cannot
     * describe. Precomputed because the map asks per column and the Mapper getters clone a {@link Location} per call.
     */
    private final int[] footprint;

    private RegionBounds(@NotNull Region region) {
        this.region = region;
        this.chunks = computeChunks(region);
        this.footprint = computeFootprint(region);
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

    @Override
    public boolean containsColumn(@NotNull World world, int x, int y, int z) {
        if (region.getWorld() != world) {
            return false;
        }
        // A shape with no known footprint keeps the height-sensitive test.
        if (footprint == null) {
            return ZoneBounds.super.containsColumn(world, x, y, z);
        }
        for (int box = 0; box < footprint.length; box += 4) {
            if (x >= footprint[box] && z >= footprint[box + 1]
                    && x <= footprint[box + 2] && z <= footprint[box + 3]) {
                return true;
            }
        }
        return false;
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

    private static int @Nullable [] computeFootprint(Region region) {
        return switch (region) {
            case CuboidRegion cuboid -> box(cuboid);
            case PolygonRegion polygon -> {
                final int[] boxes = new int[polygon.getChildren().size() * 4];
                int offset = 0;
                for (CuboidRegion child : polygon.getChildren()) {
                    System.arraycopy(box(child), 0, boxes, offset, 4);
                    offset += 4;
                }
                yield boxes;
            }
            // PerspectiveRegion extends PointRegion, so this covers both single-point shapes.
            case PointRegion point -> new int[]{point.getLocation().getBlockX(), point.getLocation().getBlockZ(),
                    point.getLocation().getBlockX(), point.getLocation().getBlockZ()};
            default -> null;
        };
    }

    private static int[] box(CuboidRegion cuboid) {
        final Location min = cuboid.getMin();
        final Location max = cuboid.getMax();
        return new int[]{min.getBlockX(), min.getBlockZ(), max.getBlockX(), max.getBlockZ()};
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
