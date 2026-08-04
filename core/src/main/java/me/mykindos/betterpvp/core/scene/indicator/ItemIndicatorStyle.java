package me.mykindos.betterpvp.core.scene.indicator;

import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.display.SceneItemDisplay;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

/**
 * An indicator drawn as a floating item — the starting point, to be replaced by a modeled style without touching
 * anything that uses it.
 */
public class ItemIndicatorStyle implements IndicatorStyle {

    private final ItemStack item;
    private final float scale;
    private final float lift;

    public ItemIndicatorStyle(@NotNull ItemStack item, float scale) {
        this(item, scale, 0f);
    }

    /**
     * @param lift how far the model sits above its own position, in blocks. A mounted indicator rides at the vehicle's
     *             head, so this is what lifts it clear of one.
     */
    public ItemIndicatorStyle(@NotNull ItemStack item, float scale, float lift) {
        this.item = item;
        this.scale = scale;
        this.lift = lift;
    }

    @Override
    public @NotNull SceneObject create(@NotNull Location at) {
        final ItemDisplay entity = at.getWorld().spawn(at, ItemDisplay.class, spawned -> {
            spawned.setPersistent(false);
            // Interpolate between positions client-side; without it a moved icon snaps rather than slides.
            spawned.setTeleportDuration(1);
        });

        final SceneItemDisplay display = new SceneItemDisplay(item, scale);
        display.init(entity);

        if (lift != 0f) {
            // Offset the model inside its own transform rather than the entity's position, so it stays put relative to
            // whatever it is riding.
            final Transformation current = entity.getTransformation();
            entity.setTransformation(new Transformation(
                    new Vector3f(0, lift, 0), current.getLeftRotation(), current.getScale(), current.getRightRotation()));
        }
        return display;
    }
}
