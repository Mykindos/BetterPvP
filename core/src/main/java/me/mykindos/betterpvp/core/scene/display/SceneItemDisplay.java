package me.mykindos.betterpvp.core.scene.display;

import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A {@link SceneObject} wrapping an {@link ItemDisplay} entity.
 * <p>
 * Configuration (item, scale, display transform) is supplied at construction time and
 * applied in {@link #onInit()} when the backing entity is bound.
 * <p>
 * Typical usage inside a {@link me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader}:
 * <pre>
 *   ItemDisplay entity = location.getWorld().spawn(location, ItemDisplay.class);
 *   spawn(new SceneItemDisplay(item, 0.5f), entity);
 * </pre>
 */
public class SceneItemDisplay extends SceneObject {

    private ItemStack item;
    private final float scale;
    private final ItemDisplay.ItemDisplayTransform transform;
    private final Quaternionf leftRotation;

    /**
     * @param item  the item to display
     * @param scale uniform scale applied to the display entity
     */
    public SceneItemDisplay(ItemStack item, float scale) {
        this(item, scale, ItemDisplay.ItemDisplayTransform.FIXED, new Quaternionf());
    }

    /**
     * @param item          the item to display
     * @param scale         uniform scale applied to the display entity
     * @param transform     how the item model is oriented ({@link ItemDisplay.ItemDisplayTransform})
     * @param leftRotation  left-side quaternion rotation (for tilting the item)
     */
    public SceneItemDisplay(ItemStack item, float scale, ItemDisplay.ItemDisplayTransform transform, Quaternionf leftRotation) {
        this.item = item;
        this.scale = scale;
        this.transform = transform;
        this.leftRotation = leftRotation;
    }

    @Override
    protected void onInit() {
        final ItemDisplay display = getItemEntity();
        display.setItemStack(item);
        display.setItemDisplayTransform(transform);
        display.setPersistent(false);
        display.setTransformation(new Transformation(
                new Vector3f(),
                leftRotation,
                new Vector3f(scale),
                new Quaternionf()
        ));
    }

    /**
     * Returns the underlying {@link ItemDisplay} entity.
     *
     * @throws IllegalStateException if {@link #init(org.bukkit.entity.Entity)} has not been called yet
     */
    public ItemDisplay getItemEntity() {
        return (ItemDisplay) getEntity();
    }

    /**
     * Updates the displayed item. Safe to call before or after {@link #init(org.bukkit.entity.Entity)};
     * if called before init the item is stored and applied in {@link #onInit()}.
     */
    public void updateItem(ItemStack newItem) {
        this.item = newItem;
        if (isInitialized()) {
            getItemEntity().setItemStack(newItem);
        }
    }
}
