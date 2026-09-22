package me.mykindos.betterpvp.core.world.schematic;

import lombok.Getter;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One structure standing in the world some number of layers tall, which can be raised, lowered or taken away.
 * <p>
 * Each layer's undo is kept separately, so lowering takes the top layers back out in reverse and leaves the rest
 * standing. Every change is also recorded under the build's id, so a restart can take a half-shown build back out
 * cleanly through {@link SchematicRenderer#open} before showing it again.
 */
public final class RenderedBuild {

    private final SchematicRenderer renderer;
    private final BlockBatchStore store;
    @Getter
    private final SchematicPlacement placement;
    @Getter
    private final LayerPlan plan;
    @Getter
    private final UUID id;
    private final List<List<Schematic.PlacedBlock>> undoByLayer = new ArrayList<>();

    RenderedBuild(@NotNull SchematicRenderer renderer, @NotNull BlockBatchStore store,
                  @NotNull SchematicPlacement placement, @NotNull LayerPlan plan, @NotNull UUID id) {
        this.renderer = renderer;
        this.store = store;
        this.placement = placement;
        this.plan = plan;
        this.id = id;
    }

    /** How many layers are standing. */
    public int getShown() {
        return undoByLayer.size();
    }

    public boolean isComplete() {
        return getShown() == plan.size();
    }

    /** Raises or lowers the build until {@code layers} of it are standing, clamped to the plan. */
    public void showUpTo(int layers) {
        final int target = Math.clamp(layers, 0, plan.size());
        if (target == getShown()) {
            return;
        }

        final World world = placement.getWorld();
        while (getShown() < target) {
            undoByLayer.add(renderer.paste(placement, plan.layer(getShown())));
        }
        while (getShown() > target) {
            renderer.restore(world, undoByLayer.removeLast());
        }
        record();
    }

    public void showAll() {
        showUpTo(plan.size());
    }

    /** Takes the whole build back out, leaving the ground as it was before the first layer went down. */
    public void revert() {
        showUpTo(0);
    }

    private void record() {
        final String world = placement.getWorld().getName();
        if (undoByLayer.isEmpty()) {
            store.clear(id, world);
            return;
        }

        final List<Schematic.PlacedBlock> undo = new ArrayList<>();
        undoByLayer.forEach(undo::addAll);
        store.save(id, world, undo);
    }
}
