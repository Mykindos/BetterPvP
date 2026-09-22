package me.mykindos.betterpvp.core.world.schematic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Writes {@link SchematicPlacement}s into the world, and takes them back out.
 * <p>
 * Every paste is the {@code //paste -a} of FAWE: only solid blocks are written, so a structure lands as its own shape
 * rather than as the cuboid it was captured in, and whatever surrounds it is left alone. Each paste returns what it
 * overwrote, the {@code //undo}, which {@link #restore} writes back. Physics is suppressed throughout so neighbouring
 * blocks do not react mid-paste.
 * <p>
 * The cost of never writing air is that the destination's own contents survive inside the structure: a hull pasted into
 * open sea keeps the sea in any space its build left empty. Build the interior out of blocks if it should be dry.
 */
@Singleton
public class SchematicRenderer {

    private final BlockBatchStore store;

    @Inject
    public SchematicRenderer(@NotNull BlockBatchStore store) {
        this.store = store;
    }

    /**
     * Pastes every solid block of {@code placement}.
     *
     * @return the blocks it overwrote, at their world positions, to hand to {@link #restore}
     */
    public @NotNull List<Schematic.PlacedBlock> paste(@NotNull SchematicPlacement placement) {
        return write(placement.getWorld(), placement.getBlocks());
    }

    /**
     * Pastes only {@code blocks}, a subset of the placement's schematic in schematic space, such as one layer of it.
     *
     * @return the blocks it overwrote, at their world positions, to hand to {@link #restore}
     */
    public @NotNull List<Schematic.PlacedBlock> paste(@NotNull SchematicPlacement placement,
                                                     @NotNull Collection<Schematic.PlacedBlock> blocks) {
        final List<Schematic.PlacedBlock> placed = new ArrayList<>(blocks.size());
        for (Schematic.PlacedBlock block : blocks) {
            if (!block.getData().getMaterial().isAir()) {
                placed.add(placement.place(block));
            }
        }
        return write(placement.getWorld(), placed);
    }

    /** Writes back blocks a paste overwrote, returning those cells to exactly what they held before it. */
    public void restore(@NotNull World world, @NotNull List<Schematic.PlacedBlock> captured) {
        for (Schematic.PlacedBlock block : captured) {
            world.getBlockAt(block.getX(), block.getY(), block.getZ()).setBlockData(block.getData(), false);
        }
    }

    /**
     * Opens a layered build of {@code placement} under {@code id}, with nothing shown yet.
     * <p>
     * If a previous run left part of this build in the world, a crash mid-construction or a world saved half built, it
     * is taken back out first from what was recorded, so the build always starts from the ground it was placed on.
     *
     * @param id identifies this build across restarts, unique within its world
     */
    public @NotNull RenderedBuild open(@NotNull SchematicPlacement placement, @NotNull LayerPlan plan, @NotNull UUID id) {
        final World world = placement.getWorld();
        store.get(id, world.getName()).ifPresent(leftover -> restore(world, leftover.getBlocks()));
        store.clear(id, world.getName());
        return new RenderedBuild(this, store, placement, plan, id);
    }

    private @NotNull List<Schematic.PlacedBlock> write(@NotNull World world, @NotNull List<Schematic.PlacedBlock> placed) {
        final List<Schematic.PlacedBlock> captured = new ArrayList<>(placed.size());
        for (Schematic.PlacedBlock block : placed) {
            final Block target = world.getBlockAt(block.getX(), block.getY(), block.getZ());
            captured.add(new Schematic.PlacedBlock(target.getX(), target.getY(), target.getZ(), target.getBlockData()));
            target.setBlockData(block.getData(), false);
        }
        return captured;
    }
}
