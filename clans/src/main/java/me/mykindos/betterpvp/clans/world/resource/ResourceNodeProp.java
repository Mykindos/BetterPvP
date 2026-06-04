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
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

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
    private final List<TextDisplay> labels = new ArrayList<>();

    public ResourceNodeProp(@NotNull SceneObjectFactory factory, @NotNull ResourceNodeDefinition definition,
                            @NotNull ResourceArchetype archetype, @NotNull Region region, int level,
                            @NotNull Zone zone, @NotNull Component label, @NotNull List<Location> labelLocations) {
        super(factory);
        this.definition = definition;
        this.archetype = archetype;
        this.region = region;
        this.level = level;
        this.zone = zone;
        this.label = label;
        this.labelLocations = List.copyOf(labelLocations);
    }

    @Override
    protected void onInit() {
        // The backing entity is the first label (spawned by the loader at labelLocations.get(0)).
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

        archetype.onActivate(this);
        addBehavior(new ArchetypeTickBehavior());
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
     * Bridges the scene tick loop to the archetype: ticks respawn each server tick and releases archetype state when
     * the node is removed.
     */
    private final class ArchetypeTickBehavior implements SceneBehavior {
        @Override
        public void tick() {
            archetype.tick(ResourceNodeProp.this);
        }

        @Override
        public void stop() {
            archetype.onDeactivate(ResourceNodeProp.this);
        }
    }
}
