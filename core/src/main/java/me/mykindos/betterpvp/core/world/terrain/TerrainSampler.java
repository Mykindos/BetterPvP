package me.mykindos.betterpvp.core.world.terrain;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Waterlogged;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Reads the cheap per-column facts a scan needs out of a world into a {@link TerrainSamples} grid, loading chunks
 * asynchronously so a large area never stalls the server.
 * <p>
 * Chunks are pulled via {@link World#getChunkAtAsync(int, int)} and captured as thread-safe {@link ChunkSnapshot}s;
 * the returned future completes once every chunk in the rectangle has been sampled. Only the surface height, the block
 * at sea level, and the top block's material are read — never the full column — which is what keeps a whole-island scan
 * affordable.
 */
public final class TerrainSampler {

    private static final Set<Material> WATER = Set.of(
            Material.WATER, Material.BUBBLE_COLUMN, Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.KELP, Material.KELP_PLANT,
            // A frozen sea surfaces as ice at sea level, so treat it as ocean water for the flood-fill.
            Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.FROSTED_ICE);

    private TerrainSampler() {
    }

    /**
     * @param world      the world to read
     * @param minX       inclusive minimum block x
     * @param minZ       inclusive minimum block z
     * @param maxX       inclusive maximum block x
     * @param maxZ       inclusive maximum block z
     * @param parameters the scan tuning (sea level, beach materials)
     * @return a future of the sampled grid, completing after every chunk has loaded and been read
     */
    public static @NotNull CompletableFuture<TerrainSamples> sample(@NotNull World world, int minX, int minZ,
                                                                    int maxX, int maxZ,
                                                                    @NotNull TerrainScanParameters parameters) {
        final TerrainSamples samples = new TerrainSamples(minX, minZ, maxX, maxZ);
        final int minChunkX = minX >> 4;
        final int maxChunkX = maxX >> 4;
        final int minChunkZ = minZ >> 4;
        final int maxChunkZ = maxZ >> 4;

        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                final int baseX = chunkX << 4;
                final int baseZ = chunkZ << 4;
                futures.add(world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk ->
                        readChunk(chunk.getChunkSnapshot(true, false, false), samples, parameters,
                                baseX, baseZ, minX, minZ, maxX, maxZ)));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> samples);
    }

    private static void readChunk(ChunkSnapshot snapshot, TerrainSamples samples, TerrainScanParameters parameters,
                                  int baseX, int baseZ, int minX, int minZ, int maxX, int maxZ) {
        for (int localX = 0; localX < 16; localX++) {
            final int worldX = baseX + localX;
            if (worldX < minX || worldX > maxX) {
                continue;
            }
            for (int localZ = 0; localZ < 16; localZ++) {
                final int worldZ = baseZ + localZ;
                if (worldZ < minZ || worldZ > maxZ) {
                    continue;
                }
                final int height = snapshot.getHighestBlockYAt(localX, localZ);
                final boolean water = WATER.contains(snapshot.getBlockType(localX, parameters.getSeaLevel(), localZ))
                        || (snapshot.getBlockData(localX, parameters.getSeaLevel(), localZ) instanceof Waterlogged waterlogged
                        && waterlogged.isWaterlogged());
                final boolean beachMaterial = parameters.getBeachMaterials()
                        .contains(snapshot.getBlockType(localX, height, localZ));
                samples.record(worldX, worldZ, height, water, beachMaterial);
            }
        }
    }
}
