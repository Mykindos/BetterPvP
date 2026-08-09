package me.mykindos.betterpvp.core.world.terrain;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Turns a grid of {@link TerrainSamples} into a {@link TerrainMask} with three connectivity-based flood-fills, none of
 * which assume anything about the island's shape:
 * <ul>
 *     <li><b>Ocean</b> grows from seed columns (and the grid's water edge) across connected water at sea level — so
 *     water the fill can't reach, like an inland pond, stays interior and safe.</li>
 *     <li><b>Mountain</b> grows from peak seeds across connected land at or above the mountain base height, tracing the
 *     real massif and stopping where it flattens.</li>
 *     <li><b>Beach</b> is the land ring within {@link TerrainScanParameters#getBeachMinWidth()} of the ocean, extended
 *     out to {@link TerrainScanParameters#getBeachMaxWidth()} along beach-material surfaces.</li>
 * </ul>
 * The result is pure data; overrides (docks, forced regions) are layered on by the caller afterwards.
 */
public final class TerrainClassifier {

    /**
     * @param world         the world the mask will belong to
     * @param samples       the sampled column grid
     * @param parameters    the scan tuning
     * @param oceanSeeds    world {@code [x, z]} points inside the open sea
     * @param mountainSeeds world {@code [x, z]} points on the mountain
     * @return the classified mask
     */
    public @NotNull TerrainMask classify(@NotNull World world, @NotNull TerrainSamples samples,
                                         @NotNull TerrainScanParameters parameters,
                                         @NotNull List<int[]> oceanSeeds, @NotNull List<int[]> mountainSeeds) {
        final int area = samples.width() * samples.depth();
        final boolean[] ocean = floodOcean(samples, oceanSeeds, area);
        final boolean[] mountain = floodMountain(samples, parameters, mountainSeeds, area);
        final int[] distanceToOcean = distanceToOcean(samples, ocean, parameters.getBeachMaxWidth(), area);

        final TerrainMask mask = new TerrainMask(world);
        for (int i = 0; i < area; i++) {
            final TerrainType type = classifyColumn(samples, parameters, ocean, mountain, distanceToOcean, i);
            if (type != TerrainType.INTERIOR) {
                mask.set(samples.worldX(i), samples.worldZ(i), type);
            }
        }
        return mask;
    }

    private TerrainType classifyColumn(TerrainSamples samples, TerrainScanParameters parameters,
                                       boolean[] ocean, boolean[] mountain, int[] distanceToOcean, int i) {
        if (mountain[i]) {
            return TerrainType.MOUNTAIN;
        }
        if (ocean[i]) {
            return TerrainType.OCEAN;
        }
        // Beach is dry land near the sea; water columns the ocean fill never reached stay interior.
        if (samples.water(i)) {
            return TerrainType.INTERIOR;
        }
        final int distance = distanceToOcean[i];
        if (distance <= 0) {
            return TerrainType.INTERIOR;
        }
        final boolean guaranteed = distance <= parameters.getBeachMinWidth();
        final boolean chased = samples.beachMaterial(i) && distance <= parameters.getBeachMaxWidth();
        return guaranteed || chased ? TerrainType.BEACH : TerrainType.INTERIOR;
    }

    private boolean[] floodOcean(TerrainSamples samples, List<int[]> seeds, int area) {
        final boolean[] ocean = new boolean[area];
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        for (int[] seed : seeds) {
            enqueueColumn(samples, ocean, queue, seed[0], seed[1]);
        }
        seedWaterEdge(samples, ocean, queue);
        flood(samples, queue, next -> !ocean[next] && samples.water(next), ocean);
        return ocean;
    }

    private boolean[] floodMountain(TerrainSamples samples, TerrainScanParameters parameters,
                                    List<int[]> seeds, int area) {
        final boolean[] mountain = new boolean[area];
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        for (int[] seed : seeds) {
            final int i = samples.index(seed[0] - samples.minX(), seed[1] - samples.minZ());
            if (i >= 0 && samples.height(i) >= parameters.getMountainBaseY() && !mountain[i]) {
                mountain[i] = true;
                queue.enqueue(i);
            }
        }
        flood(samples, queue, next -> !mountain[next] && samples.height(next) >= parameters.getMountainBaseY(), mountain);
        return mountain;
    }

    private int[] distanceToOcean(TerrainSamples samples, boolean[] ocean, int maxWidth, int area) {
        final int[] distance = new int[area];
        Arrays.fill(distance, -1);
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        for (int i = 0; i < area; i++) {
            if (ocean[i]) {
                distance[i] = 0;
                queue.enqueue(i);
            }
        }
        while (!queue.isEmpty()) {
            final int current = queue.dequeueInt();
            if (distance[current] >= maxWidth) {
                continue;
            }
            for (TerrainSamples.Direction direction : TerrainSamples.Direction.values()) {
                final int next = samples.neighbour(current, direction);
                if (next >= 0 && distance[next] < 0) {
                    distance[next] = distance[current] + 1;
                    queue.enqueue(next);
                }
            }
        }
        return distance;
    }

    private void seedWaterEdge(TerrainSamples samples, boolean[] ocean, IntArrayFIFOQueue queue) {
        final int width = samples.width();
        final int depth = samples.depth();
        for (int gridX = 0; gridX < width; gridX++) {
            enqueueGridColumn(samples, ocean, queue, gridX, 0);
            enqueueGridColumn(samples, ocean, queue, gridX, depth - 1);
        }
        for (int gridZ = 0; gridZ < depth; gridZ++) {
            enqueueGridColumn(samples, ocean, queue, 0, gridZ);
            enqueueGridColumn(samples, ocean, queue, width - 1, gridZ);
        }
    }

    private void enqueueColumn(TerrainSamples samples, boolean[] visited, IntArrayFIFOQueue queue, int worldX, int worldZ) {
        enqueueGridColumn(samples, visited, queue, worldX - samples.minX(), worldZ - samples.minZ());
    }

    private void enqueueGridColumn(TerrainSamples samples, boolean[] visited, IntArrayFIFOQueue queue, int gridX, int gridZ) {
        final int i = samples.index(gridX, gridZ);
        if (i >= 0 && !visited[i] && samples.water(i)) {
            visited[i] = true;
            queue.enqueue(i);
        }
    }

    private void flood(TerrainSamples samples, IntArrayFIFOQueue queue, Traversable canVisit, boolean[] visited) {
        while (!queue.isEmpty()) {
            final int current = queue.dequeueInt();
            for (TerrainSamples.Direction direction : TerrainSamples.Direction.values()) {
                final int next = samples.neighbour(current, direction);
                if (next >= 0 && canVisit.test(next)) {
                    visited[next] = true;
                    queue.enqueue(next);
                }
            }
        }
    }

    @FunctionalInterface
    private interface Traversable {
        boolean test(int columnIndex);
    }
}
