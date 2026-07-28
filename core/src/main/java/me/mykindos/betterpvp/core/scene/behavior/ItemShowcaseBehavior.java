package me.mykindos.betterpvp.core.scene.behavior;

import lombok.Value;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows items on fixed world positions for as long as its owner exists - wares laid out on a stall,
 * tools leaning against a wall.
 * <p>
 * Being a behaviour rather than entities parented to the NPC is what makes it chunk-safe: behaviours
 * are stopped and cleared on every dematerialization and re-attached by {@code onInit}, so the
 * displays disappear with the NPC and come back with it, without anything tracking them.
 * <p>
 * Positions are absolute, so this suits a stationary owner. A walking NPC would leave its wares
 * behind; anchor those to a model bone instead.
 */
public class ItemShowcaseBehavior implements SceneBehavior {

    /** Lays an item flat, as if resting on a surface. */
    private static final float FLAT_PITCH_DEGREES = -90.0f;

    private final SceneObject owner;
    private final List<ShowcaseItem> items;
    private final List<ItemDisplay> displays = new ArrayList<>();

    public ItemShowcaseBehavior(SceneObject owner, List<ShowcaseItem> items) {
        this.owner = owner;
        this.items = List.copyOf(items);
    }

    @Override
    public void start() {
        for (ShowcaseItem item : items) {
            final Location location = item.getLocation();
            if (location.getWorld() == null) {
                continue;
            }
            final Quaternionf rotation = new Quaternionf()
                    .rotateY((float) Math.toRadians(-location.getYaw()))
                    .rotateX((float) Math.toRadians(FLAT_PITCH_DEGREES));

            displays.add(location.getWorld().spawn(location, ItemDisplay.class, display -> {
                display.setItemStack(item.getItem());
                display.setPersistent(false);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                display.setTransformation(new Transformation(
                        new Vector3f(),
                        rotation,
                        new Vector3f(item.getScale()),
                        new Quaternionf()
                ));
            }));
        }
    }

    @Override
    public void tick() {
        // Fixed positions - nothing to follow.
    }

    @Override
    public void stop() {
        for (ItemDisplay display : displays) {
            if (!UtilEntity.isRemoved(display)) {
                display.remove();
            }
        }
        displays.clear();
    }

    /** One showcased item: what to show, where, and how big. Yaw of the location orients it. */
    @Value
    public static class ShowcaseItem {

        Location location;
        ItemStack item;
        float scale;

        public ShowcaseItem(Location location, ItemStack item, float scale) {
            this.location = location.clone();
            this.item = item;
            this.scale = scale;
        }
    }
}
