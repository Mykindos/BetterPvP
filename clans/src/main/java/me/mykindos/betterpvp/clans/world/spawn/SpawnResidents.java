package me.mykindos.betterpvp.clans.world.spawn;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import dev.brauw.mapper.Mapper;
import dev.brauw.mapper.region.PathRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.tag.PatternTag;
import dev.brauw.mapper.tag.RegionScope;
import dev.brauw.mapper.tag.TagRegistry;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.ItemShowcaseBehavior;
import me.mykindos.betterpvp.core.scene.behavior.PathfindNavigator;
import me.mykindos.betterpvp.core.scene.behavior.PatrolMode;
import me.mykindos.betterpvp.core.scene.behavior.ScriptEffectBehavior;
import me.mykindos.betterpvp.core.scene.behavior.Waypoint;
import me.mykindos.betterpvp.core.scene.behavior.WaypointPatrolBehavior;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * The people who live at spawn: stallholders standing behind their wares, and townsfolk walking the
 * streets between them.
 * <p>
 * Every resident is authored as map data, not code. A {@code npc_resident} marker declares one, its
 * tags say who it is, and it is assembled from a plain {@link ModeledNPC} plus behaviours - so adding
 * a villager needs no class, and the difference between a stallholder and a walker is only whether
 * the marker names a route.
 *
 * <h3>Data points</h3>
 * <ul>
 *   <li>{@code npc_resident} (perspective) - one villager. {@code id:} names it (only needed if it
 *       has wares), {@code skin:} picks the blueprint, {@code name:}/{@code role:} the nameplate,
 *       {@code route:} makes it walk, and {@code speed:}/{@code mode:}/{@code dwell:}/{@code nav:}
 *       tune that.</li>
 *   <li>{@code npc_route} (path) - an ordered walk, matched to a resident by {@code id:}. Waypoint
 *       order comes from the path itself; facing at each stop comes from the waypoint. The resident
 *       walks the drawn line, so a route that bends around a building needs a waypoint on the corner;
 *       tag the resident {@code nav:path} instead to have it find its own way between the points.</li>
 *   <li>{@code npc_display} (point) - one showcased item, placed exactly where it should sit.
 *       {@code resident:} says whose it is and {@code item:} which material to show.</li>
 * </ul>
 */
@CustomLog
public class SpawnResidents implements WorldContent {

    // Package-private: SpawnResidentValidator checks the same three data-points and must agree with
    // this class about what they are called.
    static final String RESIDENT_POINT = "npc_resident";
    static final String ROUTE_POINT = "npc_route";
    static final String DISPLAY_POINT = "npc_display";

    private static final String DEFAULT_MODEL = "scene_market_1";
    private static final String DEFAULT_SKIN = "skin_farmer";
    private static final String DEFAULT_IDLE = "vendor_stand_2";
    private static final String DEFAULT_WALK = "walk";
    private static final String DEFAULT_INTERACT = "vendor_stand_2_interact";

    /** Applied at every stop when a route does not say otherwise. */
    private static final long DEFAULT_DWELL_MILLIS = 3000L;

    private final SceneObjectFactory objectFactory;

    public SpawnResidents(SceneObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull Collection<Region> regions) {
        registerTags();

        final List<PathRegion> routes = MapperHelper.findRegions(regions, ROUTE_POINT, PathRegion.class);
        final List<PointRegion> displays = MapperHelper.findRegions(regions, DISPLAY_POINT, PointRegion.class);
        final List<PerspectiveRegion> markers = MapperHelper.findRegions(regions, RESIDENT_POINT, PerspectiveRegion.class);

        // Reported unconditionally: "no residents" and "residents that failed to build" look identical
        // in-world, and the counts say immediately which data-point is the one that is missing or misnamed.
        log.info("Spawn residents: {} marker(s), {} route(s), {} display(s) from {} region(s)",
                markers.size(), routes.size(), displays.size(), regions.size()).submit();

        final List<SceneSpawn> spawns = new ArrayList<>();
        for (PerspectiveRegion marker : markers) {
            marker.setWorld(world);
            spawns.add(resident(world, marker, routes, displays));
        }
        return spawns;
    }

    /**
     * Tells Mapper's in-world editor about this content: which tags these data-points take, and what
     * makes a set of them valid. Both registrations are idempotent, so re-running on reload is safe.
     */
    private void registerTags() {
        try {
            registerTagDefinitions();
            Mapper.get().getValidationRegistry().register(new SpawnResidentValidator());
        } catch (Throwable throwable) {
            // Editor convenience - it must never stop residents from spawning. Throwable, not
            // Exception, on purpose: an older Mapper on the server surfaces here as
            // NoClassDefFoundError/NoSuchMethodError, and that used to abort the whole content load.
            log.warn("Could not register spawn resident tags - check the Mapper plugin version", throwable).submit();
        }
    }

    private void registerTagDefinitions() {
        // Exact names, not a pattern: these three data-points are fixed and each takes a different set of
        // tags. A pattern scope would offer every tag on every npc_ region, so a route would advertise a
        // skin and a resident would advertise an item scale.
        final RegionScope resident = RegionScope.names(RESIDENT_POINT);
        final RegionScope display = RegionScope.names(DISPLAY_POINT);
        final RegionScope named = RegionScope.names(RESIDENT_POINT, ROUTE_POINT);

        final TagRegistry tags = Mapper.get().getTagRegistry();
        tags.register(
                // Shared - a resident and a route both carry an id, and that is how they find each other.
                new PatternTag("id", "id:.+", "id:<text>", "Identifies this resident or route", true, named),

                // Resident
                new PatternTag("name", "name:.+", "name:<text>", "Name shown on the nameplate", true, resident),
                new PatternTag("role", "role:.+", "role:<text>", "Role shown above the name", true, resident),
                new PatternTag("skin", "skin:.+", "skin:<blueprint>", "ModelEngine skin blueprint", true, resident),
                new PatternTag("model", "model:.+", "model:<blueprint>", "ModelEngine base model", true, resident),
                new PatternTag("idle", "idle:.+", "idle:<animation>", "Animation played while standing", true, resident),
                new PatternTag("walk", "walk:.+", "walk:<animation>", "Animation played while walking", true, resident),
                new PatternTag("route", "route:.+", "route:<id>", "Route this resident walks", true, resident),
                new PatternTag("speed", "speed:[0-9.]+", "speed:<number>", "Walking speed multiplier", true, resident),
                new PatternTag("mode", "mode:(circular|backtrack)", "mode:<circular|backtrack>", "How the route cycles", true, resident),
                new PatternTag("dwell", "dwell:\\d+", "dwell:<millis>", "Pause at each waypoint", true, resident),
                new PatternTag("nav", "nav:(direct|path)", "nav:<direct|path>",
                        "Walk the route exactly (direct), or pathfind between its points (path)", true, resident),

                // Display
                new PatternTag("resident", "resident:.+", "resident:<id>", "Which resident this belongs to", true, display),
                new PatternTag("item", "item:.+", "item:<material>", "Material shown on this display", true, display),
                new PatternTag("scale", "scale:[0-9.]+", "scale:<number>", "Size of the shown item", true, display));
    }

    /**
     * Assembles one resident. Everything that needs the entity present is done in a decorator, so it
     * is re-applied every time the NPC re-materializes after its chunk cycles.
     */
    private SceneSpawn resident(@NotNull World world, @NotNull PerspectiveRegion marker,
                                @NotNull List<PathRegion> routes, @NotNull List<PointRegion> displays) {
        final RegionTags tags = new RegionTags(marker.getOptions().getTags());
        final Location home = marker.getLocation();

        final String id = tags.getString("id", "");
        final String modelId = tags.getString("model", DEFAULT_MODEL);
        final String skinId = tags.getString("skin", DEFAULT_SKIN);
        final String idleAnimation = tags.getString("idle", DEFAULT_IDLE);
        final String walkAnimation = tags.getString("walk", DEFAULT_WALK);
        final String displayName = tags.getString("name", "Villager");
        final String role = tags.getString("role", "");

        final List<Waypoint> route = route(tags, routes, world);
        final List<ItemShowcaseBehavior.ShowcaseItem> wares = wares(id, displays, world);

        final ModeledNPC npc = new ModeledNPC(objectFactory);
        npc.addDecorator(object -> {
            final ModeledNPC resident = (ModeledNPC) object;
            final ModeledEntity modeled = resident.getModeledEntity();
            if (modeled == null) {
                log.warn("Spawn resident '{}' could not bind a ModeledEntity", displayName).submit();
                return;
            }

            final ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
            model.setHitboxScale(1.5);
            model.getAnimationHandler().setDefaultProperty(
                    new AnimationHandler.DefaultProperty(ModelState.IDLE, idleAnimation, 0, 0, 1));
            modeled.addModel(model, true);
            ModelEngineHelper.remapModel(model, ModelEngineAPI.getBlueprint(skinId));

            BoneTagAnchor.addNameplate(resident, model, "head", displayName,
                    Component.text(role, NamedTextColor.YELLOW));

            // Footsteps, hammer taps, sparks: whatever the animations themselves ask for.
            resident.addBehavior(new ScriptEffectBehavior(resident, model));

            if (!wares.isEmpty()) {
                resident.addBehavior(new ItemShowcaseBehavior(resident, wares));
            }
            if (route != null) {
                resident.addBehavior(patrol(resident, route, tags, walkAnimation, idleAnimation));
            }

            resident.setInteractionHandler(player -> {
                ModelEngineHelper.playAnimation(model, DEFAULT_INTERACT, 0.2, 0.1, 1.0, false);
            });
        });

        // A walker is indexed and re-spawned at the first point of its route, which is where the patrol
        // itself begins - so the NPC and its behaviour agree on where the route starts, and it never has
        // to walk in from somewhere the route does not visit. Cycling the chunk therefore restarts the
        // route from the top, which for ambient townsfolk simply reads as going back to work.
        final Location anchor = route == null ? home : route.getFirst().getLocation();
        return new SceneSpawn(npc, anchor, objectFactory::backingEntity);
    }

    private WaypointPatrolBehavior patrol(@NotNull ModeledNPC npc, @NotNull List<Waypoint> route,
                                          @NotNull RegionTags tags, @NotNull String walkAnimation,
                                          @NotNull String idleAnimation) {
        final PatrolMode mode = "backtrack".equalsIgnoreCase(tags.getString("mode", "circular"))
                ? PatrolMode.BACKTRACK
                : PatrolMode.CIRCULAR;
        final WaypointPatrolBehavior patrol = new WaypointPatrolBehavior(
                npc, route, mode, tags.getDouble("speed", 0.6), walkAnimation, idleAnimation);

        // Direct is the default because a drawn route is meant to be walked. 'path' is for a resident
        // whose route crosses ground the author cannot fully see - it reaches the same points, but
        // routes itself between them and so will not hold the drawn line.
        if ("path".equalsIgnoreCase(tags.getString("nav", "direct"))) {
            patrol.navigator(new PathfindNavigator());
        }
        return patrol;
    }

    /**
     * Resolves the route named by this resident's {@code route:} tag into waypoints, applying the
     * resident's dwell to every stop.
     *
     * @return the route, or {@code null} if the resident does not walk or names a route that is
     * missing or too short to patrol
     */
    @Nullable
    private List<Waypoint> route(@NotNull RegionTags tags, @NotNull List<PathRegion> routes, @NotNull World world) {
        final String routeId = tags.getString("route", "");
        if (routeId.isBlank()) {
            return null;
        }

        final PathRegion path = routes.stream()
                .filter(region -> routeId.equalsIgnoreCase(new RegionTags(region.getOptions().getTags()).getString("id", "")))
                .findFirst()
                .orElse(null);
        if (path == null) {
            log.warn("Spawn resident references route '{}', which no npc_route data-point declares", routeId).submit();
            return null;
        }

        path.setWorld(world);
        final long dwell = tags.getInt("dwell", (int) DEFAULT_DWELL_MILLIS);
        final List<Location> points = path.getPoints();
        if (points.size() < 2) {
            log.warn("Route '{}' has fewer than 2 waypoints - resident will stand still", routeId).submit();
            return null;
        }

        final List<Waypoint> waypoints = new ArrayList<>(points.size());
        for (Location point : points) {
            waypoints.add(new Waypoint(point, dwell));
        }
        return waypoints;
    }

    /** Collects the showcased items belonging to {@code residentId}, in map order. */
    private List<ItemShowcaseBehavior.ShowcaseItem> wares(@NotNull String residentId,
                                                          @NotNull List<PointRegion> displays,
                                                          @NotNull World world) {
        if (residentId.isBlank()) {
            return List.of();
        }

        final List<ItemShowcaseBehavior.ShowcaseItem> wares = new ArrayList<>();
        for (PointRegion display : displays) {
            final RegionTags tags = new RegionTags(display.getOptions().getTags());
            if (!residentId.equalsIgnoreCase(tags.getString("resident", ""))) {
                continue;
            }

            final Material material = Material.matchMaterial(tags.getString("item", "").toUpperCase(Locale.ROOT));
            if (material == null) {
                log.warn("Display for resident '{}' has no valid 'item' tag - skipping", residentId).submit();
                continue;
            }

            display.setWorld(world);
            wares.add(new ItemShowcaseBehavior.ShowcaseItem(
                    display.getLocation(), new ItemStack(material), (float) tags.getDouble("scale", 0.4)));
        }
        return wares;
    }
}
