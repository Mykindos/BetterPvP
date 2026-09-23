package me.mykindos.betterpvp.core.world.construction.view;

import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.construction.StructureStorage;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.schematic.LayerPlan;
import me.mykindos.betterpvp.core.world.schematic.RenderedBuild;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One structure as it appears in the world, kept in step with its status.
 * <p>
 * A build rises with its job's progress, leaving its top layers for the claim. A structure waiting to be claimed shows
 * those layers as a blinking glow and can be clicked anywhere. Once claimed, they go down one at a time. Anything that
 * changes its shape, a new stage or a move being claimed, takes the old build down and puts the new one up.
 */
final class StructureView {

    private final StructureViews views;
    private final World world;
    private final WorldContentScope scope;

    private @Nullable String shape;
    private @Nullable SchematicPlacement placement;
    private @Nullable LayerPlan plan;
    private @Nullable RenderedBuild build;
    private @Nullable StructureProp prop;
    private @Nullable ClaimFlash flash;
    private @Nullable StructureStatus lastStatus;
    private boolean animating;
    private @Nullable PlacedStructure structure;
    private List<StructureStorage.Slot> slots = List.of();
    private final Set<String> stocked = new HashSet<>();
    private boolean settled;

    StructureView(@NotNull StructureViews views, @NotNull World world, @NotNull WorldContentScope scope) {
        this.views = views;
        this.world = world;
        this.scope = scope;
    }

    void sync(@NotNull PlacedStructure structure, @NotNull StructureType type, int claimLayers, long now) {
        this.structure = structure;
        final StructureStatus status = structure.status(now);
        if (status == StructureStatus.NOT_PLACED) {
            clear();
            lastStatus = status;
            return;
        }

        final boolean claimed = lastStatus == StructureStatus.READY_TO_CLAIM
                && status != StructureStatus.READY_TO_CLAIM && status != StructureStatus.PAUSED;
        final String currentShape = structure.getType() + ":" + structure.getStage() + ":" + structure.getPosition();
        if (!currentShape.equals(shape)) {
            rebuild(structure, currentShape, claimed ? claimLayers : -1);
        }
        if (build == null || plan == null) {
            lastStatus = status;
            return;
        }

        final int reserved = plan.reservedFrom(claimLayers);
        if (claimed) {
            animating = true;
        }
        if (!animating) {
            show(target(structure, status, reserved, now));
        }

        if (status == StructureStatus.READY_TO_CLAIM && isBuilding(structure)) {
            if (flash == null) {
                flash = new ClaimFlash(world, reservedBlocks(reserved), views.getMesher());
            }
        } else if (flash != null) {
            flash.remove();
            flash = null;
        }

        if (prop != null) {
            prop.setLabel(label(type, structure, status, now));
            prop.setClaimable(status == StructureStatus.READY_TO_CLAIM);
        }
        lastStatus = status;
    }

    /** Puts the next held-back layer down, while a claim is animating in. */
    void animate() {
        if (!animating || build == null) {
            return;
        }
        show(build.getShown() + 1);
        if (build.isComplete()) {
            animating = false;
        }
    }

    void blink() {
        if (flash != null) {
            flash.blink();
        }
    }

    /** Takes the structure out of the world entirely, blocks included. Its containers are written down first. */
    void clear() {
        if (build != null) {
            show(0);
        }
        release();
    }

    /** Lets go of everything shown for it except its blocks, which stay standing in the world. */
    void release() {
        save(slots);
        if (flash != null) {
            flash.remove();
        }
        if (prop != null) {
            prop.remove();
        }
        flash = null;
        prop = null;
        build = null;
        plan = null;
        placement = null;
        shape = null;
        animating = false;
        slots = List.of();
        stocked.clear();
        settled = false;
    }

    /** Writes down the container at a world position, if it is one of this structure's and is standing. */
    boolean saveAt(int x, int y, int z) {
        for (StructureStorage.Slot slot : slots) {
            if (slot.getX() == x && slot.getY() == y && slot.getZ() == z) {
                save(List.of(slot));
                return true;
            }
        }
        return false;
    }

    /**
     * Raises or lowers the build. Containers about to come down are written down and emptied first, and containers
     * that went up are filled from the record.
     */
    private void show(int layers) {
        if (build == null) {
            return;
        }
        final List<StructureStorage.Slot> lowered = new ArrayList<>();
        for (StructureStorage.Slot slot : slots) {
            if (slot.getLayer() >= layers && stocked.contains(slot.getKey())) {
                StructureStorage.close(world, slot);
                lowered.add(slot);
            }
        }
        save(lowered);
        lowered.forEach(slot -> {
            StructureStorage.empty(world, slot);
            stocked.remove(slot.getKey());
        });

        build.showUpTo(layers);

        if (structure == null) {
            return;
        }
        for (StructureStorage.Slot slot : slots) {
            if (slot.getLayer() < build.getShown() && stocked.add(slot.getKey())) {
                StructureStorage.restore(world, structure, slot);
            }
        }
        if (build.isComplete() && !settled) {
            settled = true;
            final Location centre = placement.blockBounds().getCenter().toLocation(world);
            if (StructureStorage.settle(world, structure, slots, centre)) {
                views.changed(world);
            }
        }
    }

    private void save(@NotNull List<StructureStorage.Slot> which) {
        if (structure == null) {
            return;
        }
        boolean saved = false;
        for (StructureStorage.Slot slot : which) {
            if (stocked.contains(slot.getKey())) {
                saved |= StructureStorage.capture(world, structure, slot);
            }
        }
        if (saved) {
            views.changed(world);
        }
    }

    /** @param claimLayers when the new shape was just claimed, how many layers to leave for its animation, else -1 */
    private void rebuild(@NotNull PlacedStructure structure, @NotNull String newShape, int claimLayers) {
        clear();
        views.getShapes().placementOf(world, structure).ifPresent(found -> {
            placement = found;
            plan = LayerPlan.of(found.getSchematic());
            build = views.getRenderer().open(found, plan, structure.getId());
            slots = StructureStorage.slots(found, plan);
            shape = newShape;
            if (claimLayers >= 0) {
                show(plan.reservedFrom(claimLayers));
            }

            final BoundingBox bounds = found.blockBounds();
            final Location labelAt = new Location(world, bounds.getCenterX(), bounds.getMaxY() + 0.5, bounds.getCenterZ());
            prop = new StructureProp(views.getPropFactory(), views.getRegistry(), bounds,
                    player -> views.claim(player, world, structure.getId()));
            scope.add(new SceneSpawn(prop, labelAt, at -> at.getWorld().spawn(at, TextDisplay.class)));
        });
    }

    private int target(@NotNull PlacedStructure structure, @NotNull StructureStatus status, int reserved, long now) {
        if (!isBuilding(structure)) {
            return plan.size();
        }
        if (status == StructureStatus.READY_TO_CLAIM) {
            return reserved;
        }
        return LayerPlan.layersAt(structure.getJob().progress(now), reserved);
    }

    private static boolean isBuilding(@NotNull PlacedStructure structure) {
        final Job job = structure.getJob();
        return job != null && job.getKind() == JobKind.BUILD;
    }

    private @NotNull List<Schematic.PlacedBlock> reservedBlocks(int reserved) {
        final List<Schematic.PlacedBlock> blocks = new ArrayList<>();
        for (int layer = reserved; layer < plan.size(); layer++) {
            plan.layer(layer).forEach(block -> blocks.add(placement.place(block)));
        }
        return blocks;
    }

    private static @NotNull Component label(@NotNull StructureType type, @NotNull PlacedStructure structure,
                                            @NotNull StructureStatus status, long now) {
        final Component state = switch (status) {
            case UNDER_CONSTRUCTION -> timed(isBuilding(structure) ? "building" : "moving", structure, now);
            case ADVANCING -> timed("advancing", structure, now);
            case READY_TO_CLAIM -> Translations.component("core.construction.label.ready").color(NamedTextColor.GREEN);
            case PAUSED -> Translations.component("core.construction.label.paused").color(NamedTextColor.RED);
            case DISABLED -> structure.getJob() == null
                    ? Translations.component("core.construction.label.disabled").color(NamedTextColor.RED)
                    : timed("repairing", structure, now);
            case NEEDS_REPAIR -> structure.getJob() == null
                    ? Translations.component("core.construction.label.needs_repair").color(NamedTextColor.RED)
                    : timed("repairing", structure, now);
            case ACTIVE, NOT_PLACED -> null;
        };
        if (state == null) {
            return Component.empty();
        }
        return type.getDisplayName().decorate(TextDecoration.BOLD).appendNewline().append(state);
    }

    /** A label line saying what is being done and how long is left, under {@code core.construction.label.<doing>}. */
    private static @NotNull Component timed(@NotNull String doing, @NotNull PlacedStructure structure, long now) {
        final Duration left = Duration.ofMillis(structure.getJob().remainingMillis(now));
        return Translations.component("core.construction.label." + doing,
                Component.text(UtilTime.humanReadableFormat(left), NamedTextColor.WHITE)).color(NamedTextColor.YELLOW);
    }
}
