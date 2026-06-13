package me.mykindos.betterpvp.clans.world.resource;

import dev.brauw.mapper.region.Region;
import lombok.Getter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.scene.prop.Prop;
import me.mykindos.betterpvp.core.world.zone.Zone;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The scene object for a single resource node — its floating "Name \n Level N" labels, its gate {@link Zone}, and the
 * binding to a Mapper {@link Region} and a {@link ResourceArchetype}. One prop represents the whole node (an ore field,
 * a tree, a pond): the archetype manages whatever lies inside the zone, so a field of hundreds of ore blocks is still a
 * single scene object. It is ticked by {@code SceneTicker}, driving the archetype's respawn each tick.
 * <p>
 * Labels are operator-placed: the loader resolves one or more positions (Mapper {@code label} markers, or a single
 * auto-computed fallback). The backing entity is the first label; any extras are spawned in {@link #onInit()} and
 * attached to this prop's lifecycle. When there is more than one label, {@link ResourceNodeLabelService} shows each
 * player only the nearest.
 *
 * <h3>Chunk-managed</h3>
 * The node is registered chunk-managed: its <b>label entities</b> and its <b>respawn tick</b> materialize when the
 * node's chunk loads and dematerialize when it unloads (so the non-persistent labels survive chunk cycling instead of
 * despawning forever). The archetype's world-state lifecycle is <em>not</em> chunk-driven: {@code onActivate} (ore
 * snapshot / tree placement) runs once at load and {@code onDeactivate} once on permanent {@link #remove() removal}
 * (reload/shutdown) - re-running them per chunk cycle would reset felled trees and corrupt respawn state. The in-memory
 * snapshot persists across chunk cycles, and respawn is timestamp-based, so pausing the tick while dormant is correct
 * (it catches up on the next materialization).
 */
@Getter
public class ResourceNodeProp extends Prop {

    private static final float LABEL_SCALE = 1.2f;
    private static final float LABEL_VIEW_RANGE = 48f / 64f;

    private final ResourceNodeDefinition definition;
    private final ResourceArchetype archetype;
    private final Region region;
    private final int level;
    private final Zone zone;
    private final Component label;
    private final List<Location> labelLocations;
    private final ResourceNodeLabelService labelService;
    private final List<TextDisplay> labels = new ArrayList<>();

    public ResourceNodeProp(@NotNull SceneObjectFactory factory, @NotNull ResourceNodeDefinition definition,
                            @NotNull ResourceArchetype archetype, @NotNull Region region, int level,
                            @NotNull Zone zone, @NotNull Component label, @NotNull List<Location> labelLocations,
                            @NotNull ResourceNodeLabelService labelService) {
        super(factory);
        this.definition = definition;
        this.archetype = archetype;
        this.region = region;
        this.level = level;
        this.zone = zone;
        this.label = label;
        this.labelLocations = List.copyOf(labelLocations);
        this.labelService = labelService;
    }

    @Override
    protected void onInit() {
        // Runs on every materialization (chunk load). The backing entity is the first label, (re)spawned by the loader's
        // entity factory; any extras are spawned here and attached so they are torn down together on dematerialization.
        final TextDisplay first = (TextDisplay) getEntity();
        styleLabel(first);
        labels.add(first);

        final World world = first.getWorld();
        for (int i = 1; i < labelLocations.size(); i++) {
            final TextDisplay extra = world.spawn(labelLocations.get(i), TextDisplay.class);
            styleLabel(extra);
            attachToLifecycle(extra);
            labels.add(extra);
        }

        // Multi-label nearest-only visibility caches entity refs, which change on every materialization - (re)register
        // with the fresh displays each time. Single-label nodes render normally and are never registered.
        if (labels.size() > 1) {
            labelService.register(this);
        }

        // The archetype snapshot/placement is captured once at load by the loader (not here), so onInit only drives the
        // per-materialization respawn tick.
        addBehavior(new ArchetypeTickBehavior());
    }

    @Override
    protected void onDematerialize() {
        // Drop the stale label refs/group before the entities are removed by super; they are re-resolved on the next
        // materialization. The archetype is intentionally NOT deactivated here - only on permanent removal.
        if (labels.size() > 1) {
            labelService.unregister(this);
        }
        labels.clear();
        super.onDematerialize();
    }

    @Override
    public void remove() {
        // Permanent teardown (reload/shutdown): release the archetype's world state exactly once, mirroring the
        // load-time onActivate. This is deliberately not on dematerialize, which fires on every chunk cycle.
        archetype.onDeactivate(this);
        super.remove();
    }

    private void styleLabel(@NotNull TextDisplay display) {
        display.text(label);
        display.setBillboard(Display.Billboard.CENTER);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setShadowed(true);
        display.setSeeThrough(false);
        display.setPersistent(false);
        display.setViewRange(LABEL_VIEW_RANGE);
        display.setTransformation(new Transformation(
                new Vector3f(), new AxisAngle4f(), new Vector3f(LABEL_SCALE), new AxisAngle4f()));
    }

    /**
     * Bridges the scene tick loop to the archetype's per-tick respawn. Started on materialization and stopped on
     * dematerialization; archetype world-state release is handled by {@link #remove()}, not here, so a chunk cycle
     * (which stops this behaviour) does not deactivate the node.
     */
    private final class ArchetypeTickBehavior implements SceneBehavior {
        @Override
        public void tick() {
            archetype.tick(ResourceNodeProp.this);
        }
    }
}
