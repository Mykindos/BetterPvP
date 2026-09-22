package me.mykindos.betterpvp.core.scene.interaction;

import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes a whole box clickable on behalf of one scene object, for things far bigger than their body, such as a building.
 * <p>
 * The box is covered with {@link Interaction} entities, each aliased to the owner in the registry, so a click anywhere
 * inside reaches the owner exactly as a click on its body would (as a {@code SceneObjectInteractEvent}, and through
 * {@code Actor} if the owner is one). An interaction entity's footprint is square, so a box that is not is covered by
 * overlapping squares.
 * <p>
 * It is a behaviour, so it lives exactly as long as the owner is materialized: added in {@code onInit}, and gone when
 * the owner's chunk unloads or the owner is removed.
 */
public final class HitboxVolume implements SceneBehavior {

    /** Largest square a single interaction entity covers, so a long box is not one enormous overhanging hitbox. */
    private static final double MAX_CELL = 8.0;

    private final SceneObject owner;
    private final SceneObjectRegistry registry;
    private final BoundingBox box;
    private final List<Interaction> parts = new ArrayList<>();

    /**
     * @param owner    the object clicks are routed to. Must be materialized when this is started.
     * @param registry the registry the owner is registered in
     * @param box      the clickable area, in the owner's world
     */
    public HitboxVolume(@NotNull SceneObject owner, @NotNull SceneObjectRegistry registry, @NotNull BoundingBox box) {
        this.owner = owner;
        this.registry = registry;
        this.box = box.clone();
    }

    @Override
    public void start() {
        final World world = owner.getEntity().getWorld();
        final double cell = Math.max(0.5, Math.min(MAX_CELL, Math.min(box.getWidthX(), box.getWidthZ())));
        final int columnsX = (int) Math.ceil(box.getWidthX() / cell);
        final int columnsZ = (int) Math.ceil(box.getWidthZ() / cell);

        for (int i = 0; i < columnsX; i++) {
            for (int j = 0; j < columnsZ; j++) {
                final Location at = new Location(world, centre(box.getMinX(), box.getWidthX(), cell, columnsX, i),
                        box.getMinY(), centre(box.getMinZ(), box.getWidthZ(), cell, columnsZ, j));
                parts.add(world.spawn(at, Interaction.class, part -> {
                    part.setInteractionWidth((float) cell);
                    part.setInteractionHeight((float) box.getHeight());
                    part.setResponsive(true);
                    part.setPersistent(false);
                    part.getPersistentDataContainer().set(CoreNamespaceKeys.SCENE_OBJECT, PersistentDataType.BOOLEAN, true);
                }));
            }
        }
        parts.forEach(part -> registry.alias(part, owner));
    }

    @Override
    public void tick() {
    }

    @Override
    public void stop() {
        for (Interaction part : parts) {
            registry.unalias(part);
            part.remove();
        }
        parts.clear();
    }

    /**
     * Where the {@code index}-th of {@code count} cells is centred along one axis, spread so the first and last sit flush
     * with the box's edges and the rest overlap evenly between them.
     */
    private static double centre(double min, double width, double cell, int count, int index) {
        if (count == 1) {
            return min + width / 2;
        }
        final double step = (width - cell) / (count - 1);
        return min + cell / 2 + step * index;
    }
}
