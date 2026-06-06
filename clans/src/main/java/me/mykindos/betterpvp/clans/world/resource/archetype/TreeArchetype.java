package me.mykindos.betterpvp.clans.world.resource.archetype;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.resource.BlockBatchStore;
import me.mykindos.betterpvp.clans.world.resource.ResourceArchetype;
import me.mykindos.betterpvp.clans.world.resource.ResourceLoot;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeDefinition;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeProp;
import me.mykindos.betterpvp.clans.world.resource.Respawn;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicAnimator;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tree-felling archetype. A tree is a {@link PerspectiveRegion} marker: its location is the paste <em>anchor</em> and
 * its yaw (snapped to 90°) the rotation. The standing tree and the fell stages are authored schematics — the
 * standing schematic's {@code //copy} origin lands on the marker, so the author controls exactly which block sits
 * there and the whole tree rotates to the marker's facing.
 * <p>
 * Each {@link #onHarvest} hit counts toward {@code tree.hits}; the final hit animates the fell stages (last = stump)
 * and rolls the loot table. Frames are applied non-destructively — each is overlaid with {@link
 * SchematicAnimator#pasteCapturing} ({@code //paste -a}, solid blocks only) and the previous frame is backed out with
 * {@link SchematicAnimator#restore} ({@code //undo}) before the next is shown, so surrounding decoration is never
 * touched and exiting a frame restores exactly what it overwrote. After the respawn delay the stump is undone and the
 * standing schematic re-placed. Only the transient fell frames are mirrored to a {@link BlockBatchStore} (standing, the
 * resting state, caches nothing — like the ore store): a fresh activation restores whatever fell frame a previous run
 * left — a graceful stop, or a crash mid-animation — reverting the debris to bare ground before standing goes back down,
 * then clears the record. Standing's own cells were air, so re-airing them (config-derived) fells the tree cleanly even
 * after a restart while it stood.
 */
@Singleton
@CustomLog
public class TreeArchetype implements ResourceArchetype {

    private final Core core;
    private final ResourceLoot loot;
    private final SchematicService schematicService;
    private final SchematicAnimator schematicAnimator;
    private final BlockBatchStore frameStore;
    private final ClientManager clientManager;

    private final Map<UUID, TreePlacement> placements = new ConcurrentHashMap<>();
    private final Map<Integer, TreeRuntime> trees = new ConcurrentHashMap<>();

    @Inject
    public TreeArchetype(@NotNull Core core, @NotNull ResourceLoot loot, @NotNull SchematicService schematicService,
                         @NotNull SchematicAnimator schematicAnimator, @NotNull BlockBatchStore frameStore,
                         @NotNull ClientManager clientManager) {
        this.core = core;
        this.loot = loot;
        this.schematicService = schematicService;
        this.schematicAnimator = schematicAnimator;
        this.frameStore = frameStore;
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull String id() {
        return "tree";
    }

    @Override
    public @NotNull Region.RegionType regionType() {
        return Region.RegionType.PERSPECTIVE;
    }

    @Override
    public @NotNull CuboidRegion zoneBounds(@NotNull ResourceNodeDefinition definition, @NotNull Region region) {
        return prepare(definition, (PerspectiveRegion) region).bounds;
    }

    @Override
    public void onActivate(@NotNull ResourceNodeProp node) {
        final TreePlacement placement = prepare(node.getDefinition(), (PerspectiveRegion) node.getRegion());
        final TreeRuntime runtime = new TreeRuntime(placement);
        placeStanding(runtime, true);
        trees.put(node.getId(), runtime);
    }

    @Override
    public void onDeactivate(@NotNull ResourceNodeProp node) {
        final TreeRuntime runtime = trees.remove(node.getId());
        if (runtime != null) {
            runtime.animationGeneration++; // abort any fell frames still queued for this tree
            placements.remove(runtime.placement.regionId);

            final Location anchor = runtime.placement.anchor;
            schematicAnimator.restore(anchor.getWorld(), runtime.currentUndo);
        }
    }

    @Override
    public boolean onHarvest(@NotNull ResourceNodeProp node, @NotNull Player player, @Nullable Block block,
                             @NotNull ZoneInteraction interaction) {
        if (interaction != ZoneInteraction.BREAK) {
            return false;
        }
        final TreeRuntime runtime = trees.get(node.getId());
        if (runtime == null) {
            return false;
        }

        if (runtime.felled) {
            for (Schematic.PlacedBlock placedBlock : runtime.currentUndo) {
                if (placedBlock.getX() == block.getX() && placedBlock.getY() == block.getY() && placedBlock.getZ() == block.getZ()) {
                    return true; // this block is already air in the current frame, so the hit doesn't count
                }
            }
        }

        // Only the tree's own schematic cells fell it: a hit on terrain or decoration that merely shares the tree's
        // bounding box is ignored, so the player must actually mine the trunk/canopy.
        if (block == null || !runtime.placement.footprint.contains(pack(block.getX(), block.getY(), block.getZ()))) {
            return false;
        }

        runtime.hitCount++;
        if (runtime.hitCount < runtime.placement.hits) {
            return true; // consume the hit; the tree stands until the final blow
        }

        fell(runtime);
        final String lootTable = node.getDefinition().getLootTable();
        if (lootTable != null) {
            loot.award(lootTable, player, runtime.placement.anchor.clone());
        }
        runtime.hitCount = 0;
        return true;
    }

    @Override
    public void tick(@NotNull ResourceNodeProp node) {
        final TreeRuntime runtime = trees.get(node.getId());
        if (runtime == null || !runtime.felled || runtime.placement.standing == null) {
            return;
        }
        if (!Respawn.isReady(runtime.felledAtMs, node.getDefinition().getRespawnSeconds(), 1.0, System.currentTimeMillis())) {
            return;
        }
        placeStanding(runtime, false);
    }

    /**
     * Places the standing tree and clears the node's cache entry — standing is the resting state, so (like the ore
     * store) nothing is persisted for it. On activation ({@code clearLeftover}) a previous run may have left a fell
     * frame pasted (a graceful stop, or a crash mid-animation): its persisted undo — the terrain each fell cell
     * overwrote — is restored first, reverting the debris to bare ground; on respawn the stump is undone from the
     * in-memory undo instead. Standing is laid down with {@link SchematicAnimator#pasteCapturing} (solid blocks only,
     * decoration untouched); its own cells sat in air, so {@link TreePlacement#standingUndo} (config-derived air) is what
     * later fells it — no per-standing capture is kept. No-op without a loadable standing schematic.
     */
    private void placeStanding(@NotNull TreeRuntime runtime, boolean clearLeftover) {
        final TreePlacement placement = runtime.placement;
        if (placement.standing == null) {
            return;
        }
        final World world = placement.anchor.getWorld();
        if (world == null) {
            return;
        }
        runtime.animationGeneration++; // supersede any fell frames still scheduled
        final List<Schematic.PlacedBlock> leftover = clearLeftover
                ? frameStore.get(placement.regionId).map(BlockBatchStore.Batch::getBlocks).orElse(List.of())
                : runtime.currentUndo;
        schematicAnimator.restore(world, leftover);
        schematicAnimator.pasteCapturing(world, placement.standing, placement.anchor, placement.quarterTurns);
        runtime.currentUndo = placement.standingUndo;
        frameStore.clear(placement.regionId); // standing is the resting state — cache nothing, matching the ore store
        runtime.felled = false;

        // cues
        new SoundEffect(Sound.BLOCK_CHEST_OPEN, 0f, 1.3f).play(runtime.placement.anchor);
        new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.3f).play(runtime.placement.anchor);

        // get center location
        final double width = placement.standing.getWidth();
        final double height = placement.standing.getHeight();
        final Location center = placement.anchor.clone().add(0, height / 2, 0);

        // play particles
        Particle.ITEM_SLIME.builder()
                .location(center)
                .offset(width / 2, height / 2, width / 2)
                .count(200)
                .extra(0.1)
                .receivers(60)
                .spawn();
    }

    /**
     * Animates the fell: stage {@code i} is shown after {@code stageDelay × i} ticks by undoing the frame currently on
     * screen and overlaying the next (solid blocks only). A per-runtime generation, captured when the fell starts, lets
     * any respawn / deactivate / re-fell that bumps it abort frames still queued.
     */
    private void fell(@NotNull TreeRuntime runtime) {
        runtime.felled = true;
        runtime.felledAtMs = System.currentTimeMillis();
        final TreePlacement placement = runtime.placement;
        final World world = placement.anchor.getWorld();
        final List<Schematic> stages = placement.stages;
        if (world == null || stages.isEmpty()) {
            return;
        }

        final long generation = ++runtime.animationGeneration;
        UtilServer.repeatTask(core, run -> {
            final Schematic stage = stages.get(run);
            showFrame(runtime, world, stage, generation);

            boolean isLast = run == stages.size() - 1;
            if (isLast) {
                new SoundEffect(Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0f, 1.3f).play(runtime.placement.anchor);
            } else {
                new SoundEffect(Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0f, 1.3f).play(runtime.placement.anchor);
            }
            return true;
        }, stages.size(), placement.stageDelay);
    }

    /** Shows one fell frame: undo the frame on screen, then overlay this one — unless a newer sequence superseded us. */
    private void showFrame(@NotNull TreeRuntime runtime, @NotNull World world, @NotNull Schematic frame, long generation) {
        if (runtime.animationGeneration != generation) {
            return;
        }
        for (Schematic.PlacedBlock placedBlock : runtime.currentUndo) {
            if (Math.random() < 0.9) {
                continue;
            }
            Block block = world.getBlockAt(placedBlock.getX(), placedBlock.getY(), placedBlock.getZ());
            final BlockData data = block.getBlockData();
            UtilBlock.playBlockEffect(block, data);
        }
        schematicAnimator.restore(world, runtime.currentUndo);
        recordFrame(runtime, world, schematicAnimator.pasteCapturing(world, frame, runtime.placement.anchor, runtime.placement.quarterTurns));
    }

    /**
     * Records the undo for the frame now displayed, both in memory (to back it out at the next transition) and on disk
     * (so a restart can back out whatever frame was showing without airing the footprint and erasing decoration).
     */
    private void recordFrame(@NotNull TreeRuntime runtime, @NotNull World world, @NotNull List<Schematic.PlacedBlock> undo) {
        runtime.currentUndo = undo;
        frameStore.save(runtime.placement.regionId, world.getName(), undo);
    }

    private @NotNull TreePlacement prepare(@NotNull ResourceNodeDefinition definition, @NotNull PerspectiveRegion marker) {
        return placements.computeIfAbsent(marker.getId(), id -> compute(definition, marker));
    }

    private @NotNull TreePlacement compute(@NotNull ResourceNodeDefinition definition, @NotNull PerspectiveRegion marker) {
        final ConfigurationSection section = definition.getArchetypeSection();
        final int hits = section == null ? 3 : section.getInt("hits", 3);
        final long stageDelay = section == null ? 6L : section.getLong("stageDelay", 6L);
        final String standingName = section == null ? null : section.getString("standing");
        final List<String> stageNames = section == null ? List.of() : section.getStringList("stages");

        final World world = marker.getWorld();
        final Location raw = marker.getLocation();
        final Location anchor = new Location(world, raw.getX(), raw.getY(), raw.getZ());
        // Two corrections fold into this. (1) Minecraft yaw turns clockwise (S→W→N→E as yaw rises) but
        // SchematicAnimator.rotateXZ turns counter-clockwise, so the yaw step is negated. (2) Spigot stores the
        // perspective yaw a quarter-turn off the client facing, so a west-facing marker (the as-authored orientation)
        // must land on zero turns — the +1 phase constant. Net: a marker facing the way the schematic was saved pastes
        // it unrotated, and turning the marker turns the tree the same way.
        final int quarterTurns = Math.floorMod(1 - Math.round(marker.getYaw() / 90f), 4);

        final Schematic standing = standingName == null ? null : schematicService.load(standingName).orElse(null);
        if (standing == null) {
            throw new IllegalStateException("Tree node '" + definition.getId() + "' has no loadable 'tree.standing' schematic");
        }

        final List<Schematic> stages = new ArrayList<>();
        for (String name : stageNames) {
            final Optional<Schematic> stage = schematicService.load(name);
            if (stage.isEmpty()) {
                log.warn("Tree node '{}' is missing fell stage schematic '{}'", definition.getId(), name).submit();
            } else {
                stages.add(stage.get());
            }
        }

        final CuboidRegion bounds = computeBounds(definition, marker, standing, stages, anchor, quarterTurns, world);
        // One walk of standing's solid cells (air skipped, exactly as pasteCapturing places them), rotated to absolute
        // positions, yields both the felling footprint (which blocks a hit may break) and the config-derived undo that
        // fells the tree by re-airing those cells — they were air before the tree, so standing caches nothing.
        final BlockData air = Material.AIR.createBlockData();
        final int turns = quarterTurns & 3;
        final Set<Long> footprint = new HashSet<>();
        final List<Schematic.PlacedBlock> standingUndo = new ArrayList<>();
        for (Schematic.PlacedBlock cell : standing.getBlocks()) {
            if (cell.getData().getMaterial().isAir()) {
                continue;
            }
            final int[] rotated = SchematicAnimator.rotateXZ(cell.getX() - standing.getAnchorX(),
                    cell.getZ() - standing.getAnchorZ(), turns);
            final int wx = anchor.getBlockX() + rotated[0];
            final int wy = anchor.getBlockY() + cell.getY() - standing.getAnchorY();
            final int wz = anchor.getBlockZ() + rotated[1];
            footprint.add(pack(wx, wy, wz));
            standingUndo.add(new Schematic.PlacedBlock(wx, wy, wz, air));
        }
        return new TreePlacement(marker.getId(), standing, stages, anchor, quarterTurns, hits, stageDelay, bounds,
                footprint, standingUndo);
    }

    /** Packs a block coordinate into a single long key for footprint membership tests (matches OreArchetype's scheme). */
    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private @NotNull CuboidRegion computeBounds(@NotNull ResourceNodeDefinition definition, @NotNull PerspectiveRegion marker,
                                                @NotNull Schematic standing, @NotNull List<Schematic> stages,
                                                @NotNull Location anchor, int quarterTurns, @NotNull World world) {
        // The zone must cover every block any animation frame can occupy, not just the standing tree: a fell stage drops
        // the trunk and canopy across the ground, reaching well past the standing footprint. Were the zone bounded by the
        // standing box alone, that debris would lie outside the gate and ResourceNodeRule would never deny breaking it —
        // so the box is the union of the standing schematic and every fell stage. bounds() returns
        // [minX, minY, minZ, maxX, maxY, maxZ].
        int[] box = SchematicAnimator.bounds(standing, anchor, quarterTurns);
        for (Schematic stage : stages) {
            box = union(box, SchematicAnimator.bounds(stage, anchor, quarterTurns));
        }
        final Location min = new Location(world, box[0], box[1], box[2]);
        final Location max = new Location(world, box[3], box[4], box[5]);
        return new CuboidRegion("tree_" + marker.getId(), min, max);
    }

    /** Merges two {@code [minX, minY, minZ, maxX, maxY, maxZ]} boxes into the smallest box containing both. */
    private static int[] union(@NotNull int[] a, @NotNull int[] b) {
        return new int[] {
                Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                Math.max(a[3], b[3]), Math.max(a[4], b[4]), Math.max(a[5], b[5])
        };
    }

    /** Static placement for a tree marker: its schematics, anchor, rotation and the gate-zone footprint. */
    private static final class TreePlacement {
        private final UUID regionId;
        private final @Nullable Schematic standing;
        private final List<Schematic> stages;
        private final Location anchor;
        private final int quarterTurns;
        private final int hits;
        private final long stageDelay;
        private final CuboidRegion bounds;
        /** Packed absolute coordinates of the standing schematic's solid cells — the only blocks a hit may fell. */
        private final Set<Long> footprint;
        /** Config-derived undo that fells the tree: standing's solid cells set to air (they occupied air before it). */
        private final List<Schematic.PlacedBlock> standingUndo;

        private TreePlacement(UUID regionId, @Nullable Schematic standing, List<Schematic> stages, Location anchor,
                              int quarterTurns, int hits, long stageDelay, CuboidRegion bounds, Set<Long> footprint,
                              List<Schematic.PlacedBlock> standingUndo) {
            this.regionId = regionId;
            this.standing = standing;
            this.stages = stages;
            this.anchor = anchor;
            this.quarterTurns = quarterTurns;
            this.hits = hits;
            this.stageDelay = stageDelay;
            this.bounds = bounds;
            this.footprint = footprint;
            this.standingUndo = standingUndo;
        }
    }

    /** Per-node runtime: hit progress, felled/respawn bookkeeping, and the undo for the frame currently shown. */
    private static final class TreeRuntime {
        private final TreePlacement placement;
        private int hitCount;
        private boolean felled;
        private long felledAtMs;
        /** Blocks the currently-displayed frame overwrote, to back it out before the next frame (the {@code //undo}). */
        private List<Schematic.PlacedBlock> currentUndo = List.of();
        /** Bumped on every frame change/reset; a scheduled fell frame aborts if it no longer matches. */
        private long animationGeneration;

        private TreeRuntime(TreePlacement placement) {
            this.placement = placement;
        }
    }
}
