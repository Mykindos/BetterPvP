package me.mykindos.betterpvp.core.world.construction.view;

import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.construction.StructureStorage;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One structure as it appears in the world, kept in step with its status.
 * <p>
 * A build rises layer by layer with its job's progress and stands whole once the job is done. A structure waiting to be
 * claimed blinks with a glow and can be clicked anywhere. Anything that changes its shape, a new stage or a move being
 * claimed, takes the old build down and puts the new one up.
 * <p>
 * The pieces of the upgrades it has stand on top of the finished build, and come down before any of it does.
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
    private @Nullable PlacedStructure structure;
    private List<StructureStorage.Slot> slots = List.of();
    private final Set<String> stocked = new HashSet<>();
    private final Map<String, RenderedBuild> pieces = new HashMap<>();
    private boolean settled;

    StructureView(@NotNull StructureViews views, @NotNull World world, @NotNull WorldContentScope scope) {
        this.views = views;
        this.world = world;
        this.scope = scope;
    }

    void sync(@NotNull PlacedStructure structure, @NotNull StructureType type, long now) {
        this.structure = structure;
        final StructureStatus status = structure.status(now);
        if (status == StructureStatus.NOT_PLACED) {
            clear();
            return;
        }

        final String currentShape = structure.getType() + ":" + structure.getStage() + ":" + structure.getPosition();
        if (!currentShape.equals(shape)) {
            rebuild(structure, currentShape);
        }
        if (build == null || plan == null) {
            return;
        }

        show(target(structure, now));

        if (status == StructureStatus.READY_TO_CLAIM) {
            if (flash == null) {
                flash = new ClaimFlash(world, blocks(), views.getShell());
            }
        } else if (flash != null) {
            flash.remove();
            flash = null;
        }

        if (prop != null) {
            prop.setLabel(label(type, structure, status, now));
            prop.setClaimable(status == StructureStatus.READY_TO_CLAIM);
        }
        if (build.isComplete()) {
            showPieces(structure, type);
        }
    }

    /** Puts up the piece of every upgrade the structure has, and takes down any it no longer has. */
    private void showPieces(@NotNull PlacedStructure structure, @NotNull StructureType type) {
        final Set<String> wanted = new HashSet<>();
        for (StructureUpgrade upgrade : type.getUpgrades()) {
            if (!structure.hasUpgrade(upgrade.getId()) || upgrade.getPiece() == null) {
                continue;
            }
            wanted.add(upgrade.getId());
            if (pieces.containsKey(upgrade.getId())) {
                continue;
            }
            views.getShapes().pieceOf(world, structure, upgrade).ifPresent(placed -> {
                final UUID id = UUID.nameUUIDFromBytes((structure.getId() + ":" + upgrade.getId())
                        .getBytes(StandardCharsets.UTF_8));
                final RenderedBuild piece = views.getRenderer().open(placed, LayerPlan.of(placed.getSchematic()), id);
                piece.showAll();
                pieces.put(upgrade.getId(), piece);
            });
        }
        pieces.entrySet().removeIf(entry -> {
            if (wanted.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().revert();
            return true;
        });
    }

    private void hidePieces() {
        pieces.values().forEach(RenderedBuild::revert);
        pieces.clear();
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
        slots = List.of();
        stocked.clear();
        pieces.clear();
        settled = false;
    }

    /** The upgrade whose piece stands on a world position, if one of this structure's does. */
    @NotNull Optional<String> pieceAt(int x, int y, int z) {
        for (Map.Entry<String, RenderedBuild> piece : pieces.entrySet()) {
            if (piece.getValue().getPlacement().blockBounds().contains(x + 0.5, y + 0.5, z + 0.5)) {
                return Optional.of(piece.getKey());
            }
        }
        return Optional.empty();
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
        if (layers < plan.size()) {
            hidePieces();
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

    private void rebuild(@NotNull PlacedStructure structure, @NotNull String newShape) {
        clear();
        views.getShapes().placementOf(world, structure).ifPresent(found -> {
            placement = found;
            plan = LayerPlan.of(found.getSchematic());
            build = views.getRenderer().open(found, plan, structure.getId());
            slots = StructureStorage.slots(found, plan);
            shape = newShape;

            final BoundingBox bounds = found.blockBounds();
            final Location labelAt = new Location(world, bounds.getCenterX(), bounds.getMaxY() + 0.5, bounds.getCenterZ());
            prop = new StructureProp(views.getPropFactory(), views.getRegistry(), bounds,
                    player -> views.claim(player, world, structure.getId()));
            scope.add(new SceneSpawn(prop, labelAt, at -> at.getWorld().spawn(at, TextDisplay.class)));
        });
    }

    private int target(@NotNull PlacedStructure structure, long now) {
        if (!isBuilding(structure)) {
            return plan.size();
        }
        return LayerPlan.layersAt(structure.getJob().progress(now), plan.size());
    }

    private static boolean isBuilding(@NotNull PlacedStructure structure) {
        final Job job = structure.getJob();
        return job != null && job.getKind() == JobKind.BUILD;
    }

    private @NotNull List<Schematic.PlacedBlock> blocks() {
        final List<Schematic.PlacedBlock> blocks = new ArrayList<>();
        for (int layer = 0; layer < plan.size(); layer++) {
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
                    : underWay(structure, now);
            case NEEDS_REPAIR -> structure.getJob() == null
                    ? Translations.component("core.construction.label.needs_repair").color(NamedTextColor.RED)
                    : underWay(structure, now);
            case ACTIVE -> structure.getJob() == null ? null : underWay(structure, now);
            case NOT_PLACED -> null;
        };
        if (state == null) {
            return Component.empty();
        }
        return type.getDisplayName().decorate(TextDecoration.BOLD).appendNewline().append(state);
    }

    /** A label line for a job that leaves the structure standing as it was: a repair, or an upgrade being fitted. */
    private static @NotNull Component underWay(@NotNull PlacedStructure structure, long now) {
        return timed(structure.getJob().getKind() == JobKind.FIT_UPGRADE ? "upgrading" : "repairing", structure, now);
    }

    /** A label line saying what is being done and how long is left, under {@code core.construction.label.<doing>}. */
    private static @NotNull Component timed(@NotNull String doing, @NotNull PlacedStructure structure, long now) {
        final Duration left = Duration.ofMillis(structure.getJob().remainingMillis(now));
        return Translations.component("core.construction.label." + doing,
                Component.text(UtilTime.humanReadableFormat(left), NamedTextColor.WHITE)).color(NamedTextColor.YELLOW);
    }
}
