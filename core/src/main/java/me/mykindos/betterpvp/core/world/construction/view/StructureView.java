package me.mykindos.betterpvp.core.world.construction.view;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
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

import java.util.ArrayList;
import java.util.List;

/**
 * One structure as it appears in the world, kept in step with its status.
 * <p>
 * A build rises with its job's progress, leaving its top layers for the claim. A structure waiting to be claimed shows
 * those layers as a blinking glow and can be clicked anywhere. Once claimed, they go down one at a time. Anything that
 * changes its shape, an upgrade or a move being claimed, takes the old build down and puts the new one up.
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

    StructureView(@NotNull StructureViews views, @NotNull World world, @NotNull WorldContentScope scope) {
        this.views = views;
        this.world = world;
        this.scope = scope;
    }

    void sync(@NotNull PlacedStructure structure, @NotNull StructureType type, int claimLayers, long now) {
        final StructureStatus status = structure.status(now);
        if (status == StructureStatus.NOT_PLACED) {
            clear();
            lastStatus = status;
            return;
        }

        final boolean claimed = lastStatus == StructureStatus.READY_TO_CLAIM
                && status != StructureStatus.READY_TO_CLAIM && status != StructureStatus.PAUSED;
        final String currentShape = structure.getType() + ":" + structure.getVersion() + ":" + structure.getPosition();
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
            build.showUpTo(target(structure, status, reserved, now));
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
        build.showUpTo(build.getShown() + 1);
        if (build.isComplete()) {
            animating = false;
        }
    }

    void blink() {
        if (flash != null) {
            flash.blink();
        }
    }

    /** Takes the structure out of the world entirely, blocks included. */
    void clear() {
        if (build != null) {
            build.revert();
        }
        release();
    }

    /** Lets go of everything shown for it except its blocks, which stay standing in the world. */
    void release() {
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
    }

    /** @param claimLayers when the new shape was just claimed, how many layers to leave for its animation, else -1 */
    private void rebuild(@NotNull PlacedStructure structure, @NotNull String newShape, int claimLayers) {
        clear();
        views.getShapes().placementOf(world, structure).ifPresent(found -> {
            placement = found;
            plan = LayerPlan.of(found.getSchematic());
            build = views.getRenderer().open(found, plan, structure.getId());
            shape = newShape;
            if (claimLayers >= 0) {
                build.showUpTo(plan.reservedFrom(claimLayers));
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
            case UNDER_CONSTRUCTION -> timed(isBuilding(structure) ? "Building" : "Moving", structure, now);
            case UPGRADING -> timed("Upgrading", structure, now);
            case READY_TO_CLAIM -> Component.text("Ready - click to claim", NamedTextColor.GREEN);
            case PAUSED -> Component.text("Paused", NamedTextColor.RED);
            case DISABLED -> structure.getJob() == null ? Component.text("Disabled", NamedTextColor.RED)
                    : timed("Repairing", structure, now);
            case NEEDS_REPAIR -> structure.getJob() == null ? Component.text("Needs repair", NamedTextColor.RED)
                    : timed("Repairing", structure, now);
            case ACTIVE, NOT_PLACED -> null;
        };
        if (state == null) {
            return Component.empty();
        }
        return type.getDisplayName().decorate(TextDecoration.BOLD).appendNewline().append(state);
    }

    private static @NotNull Component timed(@NotNull String doing, @NotNull PlacedStructure structure, long now) {
        return Component.text(doing + " · ", NamedTextColor.YELLOW)
                .append(Component.text(duration(structure.getJob().remainingMillis(now)), NamedTextColor.WHITE));
    }

    private static @NotNull String duration(long millis) {
        final long seconds = (millis + 999) / 1000;
        final long hours = seconds / 3600;
        final long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes > 0 ? minutes + "m " + (seconds % 60) + "s" : seconds + "s";
    }
}
