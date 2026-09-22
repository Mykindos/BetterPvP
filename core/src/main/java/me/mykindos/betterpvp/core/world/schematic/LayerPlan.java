package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The order a structure's blocks go down in, as layers, so a building can rise a layer at a time.
 * <p>
 * By default every Y level is a layer, bottom up. A builder can override that with a Mapper cuboid tagged
 * {@code layer:<n>} captured with the structure: every block inside it goes down as part of layer key {@code n} instead
 * of its own height. Keys are ordered ascending, and an unmarked block's key is its height above the schematic's lowest
 * level, so {@code layer:-1} lands before the foundations (scaffolding) and {@code layer:1000} after the roof.
 */
@CustomLog
public final class LayerPlan {

    private static final String LAYER_TAG = "layer:";

    private final List<List<Schematic.PlacedBlock>> layers;

    private LayerPlan(@NotNull List<List<Schematic.PlacedBlock>> layers) {
        this.layers = layers;
    }

    /** Plans the solid blocks of {@code schematic}, in schematic space. */
    public static @NotNull LayerPlan of(@NotNull Schematic schematic) {
        final List<LayerOverride> overrides = overrides(schematic);
        final Map<Integer, List<Schematic.PlacedBlock>> byKey = new TreeMap<>();
        for (Schematic.PlacedBlock block : schematic.getBlocks()) {
            if (block.getData().getMaterial().isAir()) {
                continue;
            }
            byKey.computeIfAbsent(keyOf(block, schematic, overrides), key -> new ArrayList<>()).add(block);
        }

        final List<List<Schematic.PlacedBlock>> layers = new ArrayList<>(byKey.size());
        byKey.values().forEach(layer -> layers.add(Collections.unmodifiableList(layer)));
        return new LayerPlan(Collections.unmodifiableList(layers));
    }

    public int size() {
        return layers.size();
    }

    /** The blocks of layer {@code index}, in schematic space. */
    public @NotNull List<Schematic.PlacedBlock> layer(int index) {
        return layers.get(index);
    }

    /**
     * Where the last {@code reserved} layers begin, the ones held back for a finishing animation. A structure with fewer
     * layers than that reserves all of them.
     */
    public int reservedFrom(int reserved) {
        return Math.max(0, layers.size() - Math.max(0, reserved));
    }

    /** How many layers a build {@code progress} of the way through (0 to 1) shows, out of the first {@code limit}. */
    public static int layersAt(double progress, int limit) {
        return (int) Math.floor(Math.clamp(progress, 0.0, 1.0) * limit);
    }

    private static int keyOf(@NotNull Schematic.PlacedBlock block, @NotNull Schematic schematic,
                             @NotNull List<LayerOverride> overrides) {
        // LayerOverride points are anchor-relative, blocks are relative to the schematic's minimum corner.
        final int x = block.getX() - schematic.getAnchorX();
        final int y = block.getY() - schematic.getAnchorY();
        final int z = block.getZ() - schematic.getAnchorZ();
        for (LayerOverride override : overrides) {
            if (override.contains(x, y, z)) {
                return override.key;
            }
        }
        return block.getY();
    }

    private static @NotNull List<LayerOverride> overrides(@NotNull Schematic schematic) {
        final List<LayerOverride> overrides = new ArrayList<>();
        for (CapturedRegion region : schematic.getRegions()) {
            if (region.getType() != Region.RegionType.CUBOID || region.getPoints().size() < 2) {
                continue;
            }
            for (String tag : region.getTags()) {
                if (!tag.startsWith(LAYER_TAG)) {
                    continue;
                }
                try {
                    overrides.add(new LayerOverride(Integer.parseInt(tag.substring(LAYER_TAG.length()).trim()),
                            region.getPoints().get(0), region.getPoints().get(1)));
                } catch (NumberFormatException exception) {
                    log.warn("Ignoring layer override '{}' on '{}', not a whole number", tag, region.getName()).submit();
                }
            }
        }
        return overrides;
    }

    /** A cuboid of anchor-relative block positions that all go down as one layer key. */
    private static final class LayerOverride {

        private final int key;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        private LayerOverride(int key, @NotNull CapturedRegion.RelativePoint a, @NotNull CapturedRegion.RelativePoint b) {
            this.key = key;
            this.minX = (int) Math.floor(Math.min(a.getX(), b.getX()));
            this.minY = (int) Math.floor(Math.min(a.getY(), b.getY()));
            this.minZ = (int) Math.floor(Math.min(a.getZ(), b.getZ()));
            this.maxX = (int) Math.floor(Math.max(a.getX(), b.getX()));
            this.maxY = (int) Math.floor(Math.max(a.getY(), b.getY()));
            this.maxZ = (int) Math.floor(Math.max(a.getZ(), b.getZ()));
        }

        private boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }
}
