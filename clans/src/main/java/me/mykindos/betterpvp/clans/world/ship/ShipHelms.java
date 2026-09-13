package me.mykindos.betterpvp.clans.world.ship;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import dev.brauw.mapper.region.PointRegion;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSelector;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.scene.behavior.TagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.ViewerTagBehavior;
import me.mykindos.betterpvp.core.scene.prop.InteractiveProp;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import me.mykindos.betterpvp.core.world.site.crew.CrewService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The wheel of a moored ship: the thing a captain clicks to leave.
 * <p>
 * A helm is one {@code ship_helm} marker authored inside the vessel's {@code .structure}, and it needs no tags at all.
 * The point of the data-point is that being a helm <em>is</em> the configuration — where a prop has to be told that it
 * is clickable and what its click opens, a helm is only ever one thing, and a vessel authored without the right pair of
 * tags is a ship nobody can sail.
 * <p>
 * Which mooring it belongs to comes from the {@code berth:} tag {@link ShipService} stamps on everything a paste
 * places, so two hulls in one world — byte-identical copies of the same structure — each answer for their own crew.
 *
 * <h3>Data point</h3>
 * {@code ship_helm} (perspective, authored inside the structure). No tags: the wheel wears its own model, and the
 * marker's facing is which way it is mounted.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class ShipHelms implements WorldContent {

    // Package-private: ShipValidator checks the same data-point and must agree with this class about its name.
    static final String HELM_POINT = "ship_helm";

    /** The wheel a player sees and clicks. Mounted on the wall the marker faces, so its facing comes off the marker. */
    private static final String HELM_MODEL = "pirate_helm_floor";
    private static final double HELM_SCALE = 1.5;

    private final ClansSceneObjectFactory objectFactory;
    private final ShipInteractions interactions;
    private final CrewService crewService;
    private final Core core;

    /**
     * The helms that currently have a body, by {@link BerthKey}. Anything that wants to animate a wheel or bolt
     * something to it needs the live prop, and a marker on a map is not one.
     */
    private final Map<String, InteractiveProp> live = new ConcurrentHashMap<>();

    @Inject
    public ShipHelms(@NotNull ClansSceneObjectFactory objectFactory, @NotNull ShipInteractions interactions,
                     @NotNull CrewService crewService, @NotNull Core core,
                     @NotNull WorldContentService contentService) {
        this.objectFactory = objectFactory;
        this.interactions = interactions;
        this.crewService = crewService;
        this.core = core;

        // Any world: a vessel's structure is moored wherever a dock asks for it, including in cloned island instances.
        contentService.register(new WorldContentBinding(WorldSelector.any(), () -> List.of(this)));
    }

    /** The live wheel at one mooring, or empty if its chunk is unloaded or the vessel has no helm. */
    public @NotNull Optional<InteractiveProp> helm(@NotNull String worldName, @NotNull String berthId) {
        return Optional.ofNullable(live.get(key(worldName, berthId)));
    }

    /** The wheel's model, for animating it. Empty until ModelEngine has bound one. */
    public @NotNull Optional<ActiveModel> helmModel(@NotNull String worldName, @NotNull String berthId) {
        return helm(worldName, berthId)
                .map(InteractiveProp::getModeledEntity)
                .flatMap(modeled -> modeled.getModel(HELM_MODEL));
    }

    @Override
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull RegionIndex regions) {
        final List<PointRegion> markers = regions.find(HELM_POINT, PointRegion.class);
        if (markers.isEmpty()) {
            return List.of();
        }

        log.info("Helms in '{}': {} marker(s)", world.getName(), markers.size()).submit();

        final List<SceneSpawn> helms = new ArrayList<>(markers.size());
        for (PointRegion marker : markers) {
            helms.add(helm(marker));
        }
        return helms;
    }

    /**
     * Assembles one helm: an invisible hotspot on the wheel the builder made out of blocks, plus the prompt that says
     * what it is for. Both are attached in a decorator, so they come back every time the prop re-materializes after its
     * chunk cycles.
     */
    private @NotNull SceneSpawn helm(@NotNull PointRegion marker) {
        final String berthId = RegionTags.of(marker).getString(ShipService.BERTH_TAG, "");
        final Location home = marker.getLocation();

        final InteractiveProp prop = new InteractiveProp(objectFactory);
        prop.addDecorator(object -> {
            final InteractiveProp self = (InteractiveProp) object;
            self.setInteractionHandler(interactions::takeTheWheel);
            dressAsWheel(self);
            self.addBehavior(new HelmRegistration(key(home.getWorld().getName(), berthId), self));

            // Measured off the model rather than off the backing entity, which keeps its own dimensions however large
            // the wheel is drawn - so the prompt clears the helm instead of sitting inside it.
            self.addBehavior(new ViewerTagBehavior(core, TagAnchor.above(self, modelHeight() + 0.2), new Vector(),
                    viewer -> awaitingDeparture(viewer, berthId),
                    Component.translatable("clans.ship.helm-prompt").color(NamedTextColor.AQUA)));
        });

        return new SceneSpawn(prop, home, objectFactory::backingEntity);
    }

    /** Puts the wheel itself on the marker. The build underneath is a wall, so the model is what a player sees. */
    private void dressAsWheel(@NotNull InteractiveProp prop) {
        final ModeledEntity modeled = prop.getModeledEntity();
        if (modeled == null) {
            log.warn("Helm at {} could not bind a ModeledEntity for model '{}'",
                    prop.getEntity().getLocation(), HELM_MODEL).submit();
            return;
        }

        final ActiveModel model = ModelEngineAPI.createActiveModel("pirate_helm_floor");
        model.setScale(HELM_SCALE);
        model.setHitboxScale(HELM_SCALE);
        modeled.addModel(model, true);
    }

    /**
     * How tall the wheel stands, from its blueprint's own hitbox. Falls back to a block if the blueprint is missing —
     * a prompt slightly out of place beats one anchored at the deck.
     */
    private double modelHeight() {
        final ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(HELM_MODEL);
        return (blueprint == null ? 1.0 : blueprint.getMainHitbox().getHeight()) * HELM_SCALE;
    }

    /**
     * Whether this viewer is the one person the prompt is for: the captain of the crew gathered on <em>this</em> hull,
     * still in port. Anyone else either cannot set a course or is already under way, and the label would be telling
     * them to do something the wheel would refuse.
     */
    private boolean awaitingDeparture(@NotNull Player viewer, @NotNull String berthId) {
        return crewService.captainedBy(viewer.getUniqueId())
                .filter(crew -> !crew.isSailing())
                .filter(crew -> berthId.isEmpty() || crew.getBerthId().equalsIgnoreCase(berthId))
                .isPresent();
    }

    /** Berth ids are stamped lower-cased, so the lookup key is too — a caller reading one back off a marker matches. */
    private static @NotNull String key(@NotNull String worldName, @NotNull String berthId) {
        return BerthKey.of(worldName, berthId.toLowerCase(Locale.ROOT));
    }

    /**
     * Keeps {@link #live} honest. Behaviours are started when the prop materializes and stopped when it goes away, so
     * a wheel whose chunk has cycled out stops answering instead of handing back a prop with no body.
     */
    @RequiredArgsConstructor
    private class HelmRegistration implements SceneBehavior {

        private final String key;
        private final InteractiveProp prop;

        @Override
        public void start() {
            live.put(key, prop);
        }

        @Override
        public void tick() {
        }

        @Override
        public void stop() {
            live.remove(key, prop);
        }
    }
}
