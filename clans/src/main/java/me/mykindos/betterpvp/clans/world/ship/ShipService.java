package me.mykindos.betterpvp.clans.world.ship;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.Mapper;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.tag.PatternTag;
import dev.brauw.mapper.tag.RegionScope;
import dev.brauw.mapper.tag.TagRegistry;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.content.RegionContributor;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.site.ArrivalPoints;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicAnimator;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.schematic.StructureAnchor;
import me.mykindos.betterpvp.core.world.schematic.StructureTransform;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Places ships at their berths and answers which hull a player is standing on.
 * <p>
 * A dock authors one {@code ship_berth} marker per mooring; the hull, its helm, its crew NPC and its bounds all arrive
 * from the named {@code .structure}. That is what keeps a fleet of docks from being a fleet of hand-built ships: the
 * vessel is authored once and moored wherever a marker says.
 * <p>
 * Runs as a {@link RegionContributor}, so the data-points it pastes reach the props, residents and helm systems as
 * ordinary markers — a wheel authored inside the hull is found exactly as if it had been drawn in the dock's own map.
 *
 * <h3>Data points</h3>
 * In the dock world, {@code ship_berth} (perspective):
 * <ul>
 *   <li>{@code id:} names this mooring, so a navigator can point at it</li>
 *   <li>{@code structure:} which {@code .structure} to moor here</li>
 * </ul>
 * The marker is matched against the structure's {@code structure_anchor}, so its position and facing decide where the
 * hull sits and which way it points. Put it one block above the water and the ship floats at the depth it was built
 * at, with nothing measured by hand.
 * <p>
 * Inside the structure, and therefore authored once for the vessel rather than at every dock:
 * <ul>
 *   <li>{@code structure_anchor} (perspective) — the stern, at the waterline, facing along the hull; carries
 *       {@code capacity:}</li>
 *   <li>{@code ship_board} (perspective) — where boarders land, facing the helm</li>
 *   <li>{@code ship_helm} (perspective) — the wheel, facing out of the wall it is mounted on; see {@link ShipHelms}</li>
 * </ul>
 * The hull's extent is not authored at all: it is the selection the {@code .structure} was captured from, turned and
 * moved to wherever it is moored. A volume drawn a second time as a data-point would only be another thing to keep in
 * step with the build.
 */
@Singleton
@CustomLog
public class ShipService implements RegionContributor {

    public static final String BERTH_POINT = "ship_berth";

    /** The dock NPC that boards players; carries {@code ship:<berth id>}. */
    public static final String NAVIGATOR_POINT = "npc_navigator";

    /** Stamped onto every region a paste places, so two hulls in one world stay apart. */
    static final String BERTH_TAG = "berth";

    /** The interaction name the vessel's crew contact carries. */
    public static final String CREW_INTERACTION = "crew";

    /**
     * Where boarders land, authored inside the structure.
     * <p>
     * Separate from the anchor because the two want opposite things: the anchor sits at the stern on the waterline so
     * the hull floats at the right depth, which is the last place to drop somebody.
     */
    static final String BOARD_POINT = "ship_board";

    private static final int DEFAULT_CAPACITY = 8;

    private boolean tagsRegistered;

    private final SchematicService schematics;
    private final SchematicAnimator animator;

    private final Map<String, List<Berth>> berthsByWorld = new HashMap<>();

    /**
     * Vessels assigned to empty moorings at runtime, keyed by {@link BerthKey}.
     * <p>
     * A berth with no {@code structure:} tag is a mooring waiting for a ship rather than a broken one — which is what
     * the open sea is. Consulted on every load, so re-running content re-pastes whatever was assigned instead of
     * emptying the berth again.
     */
    private final Map<String, String> assignments = new ConcurrentHashMap<>();

    /** What each world held before its ships were pasted, so the blocks can be handed back. */
    private final Map<String, List<Schematic.PlacedBlock>> undo = new ConcurrentHashMap<>();

    /**
     * Moorings temporarily forced empty, keyed by {@link BerthKey}.
     * <p>
     * Beats both the authored {@code structure:} tag and any runtime assignment, which is the point: this exists so a
     * builder can see the dock underneath a hull that the map insists belongs there.
     */
    private final Set<String> suppressed = ConcurrentHashMap.newKeySet();

    /** Pending automatic re-moorings, so a manual {@link #enable(World, String)} can cancel one. */
    private final Map<String, BukkitTask> expiries = new ConcurrentHashMap<>();

    private final Clans clans;
    private final WorldContentService contentService;

    @Inject
    public ShipService(@NotNull SchematicService schematics, @NotNull SchematicAnimator animator,
                       @NotNull WorldContentService contentService, @NotNull Clans clans) {
        this.schematics = schematics;
        this.animator = animator;
        this.contentService = contentService;
        this.clans = clans;
        contentService.registerRegions(this);
    }

    @Override
    public @NotNull List<Region> contribute(@NotNull World world, @NotNull Collection<Region> authored) {
        registerTags();

        // Put back whatever the last pass overwrote before writing again, so repeated loads cannot layer hulls on top
        // of each other and lose the original ground.
        restore(world);

        final List<Berth> berths = new ArrayList<>();
        final List<Region> placed = new ArrayList<>();

        for (Region region : authored) {
            if (!BERTH_POINT.equalsIgnoreCase(region.getName()) || !(region instanceof PerspectiveRegion marker)) {
                continue;
            }
            marker.setWorld(world);
            moor(world, marker, placed).ifPresent(berths::add);
        }

        berthsByWorld.put(world.getName(), berths);
        if (!berths.isEmpty()) {
            log.info("Moored {} ship(s) in '{}'", berths.size(), world.getName()).submit();
        }
        return placed;
    }

    /** Every berth currently moored in a world. */
    public @NotNull List<Berth> berths(@NotNull World world) {
        return berthsByWorld.getOrDefault(world.getName(), List.of());
    }

    public @NotNull Optional<Berth> berth(@NotNull World world, @NotNull String id) {
        return berths(world).stream().filter(berth -> berth.getId().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * Moors {@code structureName} at an empty berth. Takes effect on the next content load for that world, which the
     * caller triggers — pasting a hull is only half of it, since the helm and crew it carries have to be installed too.
     */
    public void assign(@NotNull World world, @NotNull String berthId, @NotNull String structureName) {
        assignments.put(BerthKey.of(world.getName(), berthId), structureName);
    }

    /**
     * Un-moors a berth and hands its blocks back, leaving the dock as the builder made it.
     * <p>
     * The world is rebuilt rather than merely un-pasted: a hull carries its helm, its crew contact and its props as
     * data-points, so pulling the blocks out on their own would leave a wheel you can steer floating over open water.
     * Going through the content pipeline takes the whole vessel away together.
     *
     * @param millis how long until it moors itself again; zero or less to leave it off until told otherwise
     */
    public void disable(@NotNull World world, @NotNull Collection<String> berthIds, long millis) {
        for (String berthId : berthIds) {
            final String key = BerthKey.of(world.getName(), berthId);
            suppressed.add(key);
            cancelExpiry(key);
            if (millis > 0) {
                expiries.put(key, scheduleEnable(world.getName(), berthId, millis));
            }
        }
        contentService.loadWorld(world);
    }

    /** Moors disabled berths again, cancelling any pending automatic re-mooring. */
    public void enable(@NotNull World world, @NotNull Collection<String> berthIds) {
        boolean changed = false;
        for (String berthId : berthIds) {
            final String key = BerthKey.of(world.getName(), berthId);
            cancelExpiry(key);
            changed |= suppressed.remove(key);
        }
        if (changed) {
            contentService.loadWorld(world);
        }
    }

    /**
     * Re-moors one berth once its disabled spell is up. Keyed by world <em>name</em> rather than holding the
     * {@link World}: a discovery island can be handed back and unloaded while its ships are off, and the timer must not
     * pin a dead world in memory.
     */
    private @NotNull BukkitTask scheduleEnable(@NotNull String worldName, @NotNull String berthId, long millis) {
        return UtilServer.runTaskLater(clans, () -> {
            final World current = Bukkit.getWorld(worldName);
            if (current != null) {
                enable(current, List.of(berthId));
            } else {
                final String key = BerthKey.of(worldName, berthId);
                suppressed.remove(key);
                expiries.remove(key);
            }
        }, millis / 50);
    }

    /** Whether this mooring is being held empty rather than being empty in the map. */
    public boolean isDisabled(@NotNull World world, @NotNull String berthId) {
        return suppressed.contains(BerthKey.of(world.getName(), berthId));
    }

    /** Empties every mooring in a world. Called when an instance is handed back so the next voyage starts clean. */
    public void clearAssignments(@NotNull String worldName) {
        assignments.keySet().removeIf(key -> BerthKey.worldOf(key).equals(worldName));
        suppressed.removeIf(key -> {
            if (!BerthKey.worldOf(key).equals(worldName)) {
                return false;
            }
            cancelExpiry(key);
            return true;
        });
        berthsByWorld.remove(worldName);
    }

    private void cancelExpiry(@NotNull String key) {
        final BukkitTask pending = expiries.remove(key);
        if (pending != null) {
            pending.cancel();
        }
    }

    /**
     * Puts back every block a paste in {@code world} overwrote, newest first.
     * <p>
     * Reverse order because two hulls may overlap: undoing them in the order they were written would restore an older
     * copy of a shared cell over a newer one.
     */
    public void restore(@NotNull World world) {
        final List<Schematic.PlacedBlock> captured = undo.remove(world.getName());
        if (captured == null || captured.isEmpty()) {
            return;
        }

        final List<Schematic.PlacedBlock> reversed = new ArrayList<>(captured);
        Collections.reverse(reversed);
        animator.restore(world, reversed);
    }

    /**
     * Un-pastes every ship on the server. Called on shutdown so the hulls never reach disk — a dock world should hold
     * the dock a builder made, not a vessel the server drew on top of it.
     */
    public void restoreAll() {
        for (String worldName : new ArrayList<>(undo.keySet())) {
            final World world = Bukkit.getWorld(worldName);
            if (world != null) {
                restore(world);
            } else {
                undo.remove(worldName);
            }
        }
    }

    /** Which hull {@code location} is inside, if any — the test for "are they on the ship". */
    public @NotNull Optional<Berth> berthAt(@NotNull Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return berths(location.getWorld()).stream().filter(berth -> berth.contains(location)).findFirst();
    }

    /**
     * Pastes one berth's structure and builds its {@link Berth}.
     * <p>
     * The hull is turned by the difference between the structure's captured facing and the marker's, so a berth pointing
     * up-river moors the ship along it without the builder rotating anything by hand.
     */
    private @NotNull Optional<Berth> moor(@NotNull World world, @NotNull PerspectiveRegion marker,
                                          @NotNull List<Region> placed) {
        final RegionTags tags = RegionTags.of(marker);
        final String id = tags.getString("id", "").trim();
        if (id.isEmpty()) {
            log.warn("A {} in '{}' has no 'id' tag - nothing can refer to it", BERTH_POINT, world.getName()).submit();
            return Optional.empty();
        }

        final Location anchor = marker.getLocation();

        // No structure named, and none assigned: an empty mooring. The open sea is exactly this - somewhere a ship can
        // be put later, rather than somewhere a particular ship lives.
        final String structureName = vesselFor(world, id, tags);
        if (structureName.isEmpty()) {
            return Optional.of(new Berth(id, world.getName(), "", anchor, anchor, null, DEFAULT_CAPACITY, null));
        }

        final Optional<Schematic> loaded = schematics.load(structureName);
        if (loaded.isEmpty()) {
            log.warn("Berth '{}' in '{}' names structure '{}', which does not exist",
                    id, world.getName(), structureName).submit();
            return Optional.empty();
        }

        final Schematic schematic = loaded.get();
        final int turns = StructureTransform.quarterTurnsBetween(schematic.getAnchorYaw(), anchor.getYaw());
        final BoundingBox hull = StructureTransform.pastedBounds(schematic, anchor, turns);

        // Captured as it is written, so the world can be handed back exactly as it was found. A pasted hull is scenery
        // the server puts there each boot, not something that should be saved into the map and outlive the plugin.
        undo.computeIfAbsent(world.getName(), key -> new ArrayList<>())
                .addAll(animator.pasteCapturing(world, schematic, anchor, turns));

        final List<Region> regions = animator.pasteRegions(schematic, anchor, turns,
                Set.of(BERTH_TAG + ":" + id.toLowerCase(Locale.ROOT)));
        placed.addAll(regions);

        // Falls back to the berth marker so a structure with no boarding point still puts people somewhere sensible
        // rather than refusing to be boarded.
        final Location board = perspectiveNamed(regions, BOARD_POINT).orElse(anchor);
        return Optional.of(new Berth(id, world.getName(), structureName, anchor, board, hull, capacityOf(regions),
                quartermasterAt(regions)));
    }

    private static @NotNull Optional<Location> perspectiveNamed(@NotNull List<Region> regions, @NotNull String name) {
        return regions.stream()
                .filter(region -> name.equalsIgnoreCase(region.getName()))
                .filter(PerspectiveRegion.class::isInstance)
                .map(region -> ((PerspectiveRegion) region).getLocation())
                .findFirst();
    }

    /**
     * Finds the resident the structure marked as the crew contact. Located by its {@code interact:} action rather than
     * by name, so a vessel can call them whatever suits it.
     */
    private static Location quartermasterAt(@NotNull List<Region> regions) {
        return regions.stream()
                .filter(region -> region instanceof PerspectiveRegion)
                .filter(region -> CREW_INTERACTION.equalsIgnoreCase(RegionTags.of(region).getString("interact", "")))
                .map(region -> ((PerspectiveRegion) region).getLocation())
                .findFirst()
                .orElse(null);
    }

    /**
     * Tells Mapper's in-world editor which tags the ship data-points take, and what makes a set of them valid.
     * <p>
     * Done once, on the first world that asks: the definitions are the same everywhere, and this runs against every
     * world on the server.
     */
    private void registerTags() {
        if (tagsRegistered) {
            return;
        }
        tagsRegistered = true;

        try {
            registerTagDefinitions();
            Mapper.get().getValidationRegistry().register(new ShipValidator());
        } catch (Throwable throwable) {
            // Editor convenience only - it must never stop ships from being moored. Throwable, not Exception: an older
            // Mapper surfaces here as NoClassDefFoundError/NoSuchMethodError.
            log.warn("Could not register ship tags - check the Mapper plugin version", throwable).submit();
        }
    }

    private void registerTagDefinitions() {
        // Exact names rather than a pattern: these data-points take different tags, and a pattern scope would offer a
        // hull's capacity on a navigator.
        final RegionScope berth = RegionScope.names(BERTH_POINT);
        final RegionScope navigator = RegionScope.names(NAVIGATOR_POINT);
        final RegionScope vessel = RegionScope.names(StructureAnchor.POINT);
        final RegionScope arrival = RegionScope.names(ArrivalPoints.DEFAULT_MARKER);

        final TagRegistry tags = Mapper.get().getTagRegistry();
        tags.register(
                // Berth
                new PatternTag("id", "id:.+", "id:<text>", "Names this mooring so a navigator can point at it", true, berth),
                new PatternTag("structure", "structure:.+", "structure:<name>",
                        "Which .structure is moored here. Omit for an empty mooring, filled at runtime", true, berth),

                // Navigator
                new PatternTag("ship", "ship:.+", "ship:<berth id>", "The mooring this navigator boards you onto", true, navigator),

                // Authored on the vessel's own anchor, so it travels with the structure rather than with a dock
                new PatternTag("capacity", "capacity:\\d+", "capacity:<number>",
                        "How many can sail on this vessel, captain included", true, vessel),

                // Arrival
                new PatternTag("name", "name:.+", "name:<text>",
                        "What this landing is called, for a destination that picks a fixed one", true, arrival));
    }

    /**
     * Which vessel belongs at this berth: the one the map names, or the one assigned at runtime.
     * <p>
     * The map wins, so a dock's ship cannot be swapped out from under it; only a berth left deliberately empty is
     * fillable. A disabled berth resolves to nothing at all, which the rest of the system already understands as an
     * empty mooring — so turning a ship off costs no second teardown path.
     */
    private @NotNull String vesselFor(@NotNull World world, @NotNull String id, @NotNull RegionTags tags) {
        if (suppressed.contains(BerthKey.of(world.getName(), id))) {
            return "";
        }

        final String authored = tags.getString("structure", "").trim();
        if (!authored.isEmpty()) {
            return authored;
        }
        return assignments.getOrDefault(BerthKey.of(world.getName(), id), "");
    }

    /**
     * Capacity comes off the vessel's own anchor, so one ship holds the same number wherever it is moored — and so the
     * marker that already has to exist for a structure to paste is the one that carries it.
     */
    private static int capacityOf(@NotNull List<Region> regions) {
        return regions.stream()
                .filter(region -> StructureAnchor.POINT.equalsIgnoreCase(region.getName()))
                .findFirst()
                .map(anchor -> Math.max(1, RegionTags.of(anchor).getInt("capacity", DEFAULT_CAPACITY)))
                .orElse(DEFAULT_CAPACITY);
    }
}
