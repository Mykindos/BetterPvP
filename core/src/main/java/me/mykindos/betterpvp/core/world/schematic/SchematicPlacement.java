package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.Region;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A schematic placed somewhere: its anchor block on {@link #getAnchor()}, turned {@link #getQuarterTurns()} times.
 * <p>
 * Everything about where a structure ends up is read from here, so the paste, its bounds, its footprint and its markers
 * cannot disagree. Air is never part of a placement: a structure lands as its own shape, not as the cuboid it was
 * captured in.
 */
@Getter
public final class SchematicPlacement {

    private final Schematic schematic;
    private final Location anchor;
    private final int quarterTurns;

    private @Nullable List<Schematic.PlacedBlock> blocks;
    private @Nullable Footprint footprint;

    private SchematicPlacement(@NotNull Schematic schematic, @NotNull Location anchor, int quarterTurns) {
        this.schematic = schematic;
        this.anchor = anchor.toBlockLocation();
        this.quarterTurns = Math.floorMod(quarterTurns, 4);
    }

    public static @NotNull SchematicPlacement of(@NotNull Schematic schematic, @NotNull Location anchor, int quarterTurns) {
        return new SchematicPlacement(schematic, anchor, quarterTurns);
    }

    /** Placed on {@code anchor} and turned to face the way {@code anchor} looks. */
    public static @NotNull SchematicPlacement facing(@NotNull Schematic schematic, @NotNull Location anchor) {
        return of(schematic, anchor, BlockTransform.quarterTurnsBetween(schematic.getAnchorYaw(), anchor.getYaw()));
    }

    /** The same schematic somewhere else. */
    public @NotNull SchematicPlacement moveTo(@NotNull Location anchor, int quarterTurns) {
        return of(schematic, anchor, quarterTurns);
    }

    public @NotNull World getWorld() {
        return anchor.getWorld();
    }

    /** Where one of the schematic's own blocks lands, with its facing turned to match. */
    public @NotNull Schematic.PlacedBlock place(@NotNull Schematic.PlacedBlock block) {
        final int[] rotated = BlockTransform.rotateBlock(block.getX() - schematic.getAnchorX(),
                block.getZ() - schematic.getAnchorZ(), quarterTurns);
        return new Schematic.PlacedBlock(anchor.getBlockX() + rotated[0],
                anchor.getBlockY() + block.getY() - schematic.getAnchorY(),
                anchor.getBlockZ() + rotated[1],
                BlockTransform.rotateData(block.getData(), quarterTurns));
    }

    /** Every solid block of the schematic, at its world position, in schematic order. */
    public @NotNull List<Schematic.PlacedBlock> getBlocks() {
        if (blocks == null) {
            final List<Schematic.PlacedBlock> placed = new ArrayList<>(schematic.getBlocks().size());
            for (Schematic.PlacedBlock block : schematic.getBlocks()) {
                if (!block.getData().getMaterial().isAir()) {
                    placed.add(place(block));
                }
            }
            blocks = Collections.unmodifiableList(placed);
        }
        return blocks;
    }

    /** The columns and height the solid blocks occupy. */
    public @NotNull Footprint getFootprint() {
        if (footprint == null) {
            footprint = footprintOf(getBlocks());
        }
        return footprint;
    }

    /**
     * The smallest box around the solid blocks, spanning whole blocks. An empty schematic yields the anchor block.
     */
    public @NotNull BoundingBox blockBounds() {
        final List<Schematic.PlacedBlock> placed = getBlocks();
        if (placed.isEmpty()) {
            return BoundingBox.of(anchor.getBlock());
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Schematic.PlacedBlock block : placed) {
            minX = Math.min(minX, block.getX());
            minY = Math.min(minY, block.getY());
            minZ = Math.min(minZ, block.getZ());
            maxX = Math.max(maxX, block.getX());
            maxY = Math.max(maxY, block.getY());
            maxZ = Math.max(maxZ, block.getZ());
        }
        return new BoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    /**
     * The captured selection itself, turned and moved to where it lands, air included.
     * <p>
     * This is the box a builder drew around the build, which is what "is somebody inside this structure" should ask
     * about: a ship's deck is aboard even where the build left it open. {@link #blockBounds()} is the tight box around
     * what was actually built.
     */
    public @NotNull BoundingBox selectionBounds() {
        final int minX = -schematic.getAnchorX();
        final int minZ = -schematic.getAnchorZ();
        final int maxX = schematic.getWidth() - 1 - schematic.getAnchorX();
        final int maxZ = schematic.getLength() - 1 - schematic.getAnchorZ();

        // All four corners, because a quarter turn swaps which pair of them is opposite: rotating only min and max
        // gives the right box for a half turn and a sheared one for a quarter.
        final int[][] corners = {
                BlockTransform.rotateBlock(minX, minZ, quarterTurns),
                BlockTransform.rotateBlock(minX, maxZ, quarterTurns),
                BlockTransform.rotateBlock(maxX, minZ, quarterTurns),
                BlockTransform.rotateBlock(maxX, maxZ, quarterTurns)};

        int lowX = corners[0][0];
        int highX = corners[0][0];
        int lowZ = corners[0][1];
        int highZ = corners[0][1];
        for (int[] corner : corners) {
            lowX = Math.min(lowX, corner[0]);
            highX = Math.max(highX, corner[0]);
            lowZ = Math.min(lowZ, corner[1]);
            highZ = Math.max(highZ, corner[1]);
        }

        final int baseY = anchor.getBlockY() - schematic.getAnchorY();
        return new BoundingBox(
                anchor.getBlockX() + lowX, baseY, anchor.getBlockZ() + lowZ,
                anchor.getBlockX() + highX + 1, baseY + schematic.getHeight(), anchor.getBlockZ() + highZ + 1);
    }

    /**
     * The schematic's captured data-points, positioned and turned to match the blocks. They are not part of the
     * world's Mapper file, so whoever placed the structure hands them to the content pipeline for this session only.
     */
    public @NotNull List<Region> markers() {
        return markers(Set.of());
    }

    /**
     * As {@link #markers()}, tagging each one so this placement can be told apart from another copy of the same
     * structure in the world.
     */
    public @NotNull List<Region> markers(@NotNull Set<String> extraTags) {
        final List<Region> placed = new ArrayList<>(schematic.getRegions().size());
        for (CapturedRegion region : schematic.getRegions()) {
            placed.add(region.rebuild(anchor, quarterTurns, extraTags));
        }
        return placed;
    }

    static @NotNull Footprint footprintOf(@NotNull Collection<Schematic.PlacedBlock> placed) {
        final LongSet columns = new LongOpenHashSet(placed.size());
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Schematic.PlacedBlock block : placed) {
            columns.add(Footprint.pack(block.getX(), block.getZ()));
            minY = Math.min(minY, block.getY());
            maxY = Math.max(maxY, block.getY());
        }
        return placed.isEmpty() ? new Footprint(columns, 0, -1) : new Footprint(columns, minY, maxY);
    }
}
