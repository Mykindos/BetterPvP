package me.mykindos.betterpvp.core.world.terrain;

import org.jetbrains.annotations.NotNull;

/**
 * A rectangular grid of the cheap per-column facts a terrain scan needs — surface height, whether the column is water at
 * sea level, and whether its surface is a beach material — sampled once from the world so the {@link TerrainClassifier}
 * can run its flood-fills purely in memory, off the main thread.
 * <p>
 * Columns are addressed by world block coordinates within {@code [minX..maxX] × [minZ..maxZ]} and stored row-major by z.
 */
public final class TerrainSamples {

    private final int minX;
    private final int minZ;
    private final int width;
    private final int depth;
    private final int[] height;
    private final boolean[] water;
    private final boolean[] beachMaterial;

    public TerrainSamples(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.width = maxX - minX + 1;
        this.depth = maxZ - minZ + 1;
        final int area = width * depth;
        this.height = new int[area];
        this.water = new boolean[area];
        this.beachMaterial = new boolean[area];
    }

    public int minX() {
        return minX;
    }

    public int minZ() {
        return minZ;
    }

    public int width() {
        return width;
    }

    public int depth() {
        return depth;
    }

    /** @return the flat array index for a grid cell, or {@code -1} if outside the sampled rectangle */
    public int index(int gridX, int gridZ) {
        if (gridX < 0 || gridZ < 0 || gridX >= width || gridZ >= depth) {
            return -1;
        }
        return gridZ * width + gridX;
    }

    public void record(int worldX, int worldZ, int surfaceHeight, boolean isWater, boolean isBeachMaterial) {
        final int i = index(worldX - minX, worldZ - minZ);
        if (i < 0) {
            return;
        }
        height[i] = surfaceHeight;
        water[i] = isWater;
        beachMaterial[i] = isBeachMaterial;
    }

    public int height(int i) {
        return height[i];
    }

    public boolean water(int i) {
        return water[i];
    }

    public boolean beachMaterial(int i) {
        return beachMaterial[i];
    }

    public int worldX(int i) {
        return minX + (i % width);
    }

    public int worldZ(int i) {
        return minZ + (i / width);
    }

    /** @return the grid-space neighbour index in a cardinal direction, or {@code -1} if it leaves the grid */
    public int neighbour(int i, @NotNull Direction direction) {
        final int gridX = i % width;
        final int gridZ = i / width;
        return index(gridX + direction.dx, gridZ + direction.dz);
    }

    /** The four cardinal directions, used for grid flood-fills. */
    public enum Direction {
        NORTH(0, -1),
        SOUTH(0, 1),
        EAST(1, 0),
        WEST(-1, 0);

        private final int dx;
        private final int dz;

        Direction(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }
    }
}
