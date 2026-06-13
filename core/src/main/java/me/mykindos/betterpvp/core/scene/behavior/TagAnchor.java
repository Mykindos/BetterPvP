package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies the world position a {@link TagBehavior} display should follow.
 * <p>
 * Resolved every tick, so implementations may track moving targets (entities,
 * model bones, etc.). Returning {@code null} means the anchor is currently
 * unavailable - the display simply stays where it is until the anchor resolves again.
 */
@FunctionalInterface
public interface TagAnchor {

    /**
     * @return a fresh {@link Location} for the tag's base point (safe for the caller
     *         to mutate), or {@code null} if the anchor cannot currently be resolved.
     */
    @Nullable
    Location resolve();

    /**
     * Anchors 0.2 blocks above the top of the owner entity's bounding box.
     * This is the default anchor used by {@link TagBehavior}.
     */
    static TagAnchor aboveEntity(SceneObject owner) {
        return () -> {
            if (!owner.isInitialized()) {
                return null;
            }
            final Entity entity = owner.getEntity();
            return entity.getLocation().add(0, entity.getHeight() + 0.2, 0);
        };
    }
}
