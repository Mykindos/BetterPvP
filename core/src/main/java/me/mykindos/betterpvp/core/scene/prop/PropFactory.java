package me.mykindos.betterpvp.core.scene.prop;

import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract factory for command-spawnable {@link Prop} types.
 * <p>
 * Analogous to {@link me.mykindos.betterpvp.core.npc.NPCFactory}: subclasses implement
 * {@link #spawnDefault(org.bukkit.Location, String)} and use {@link #spawnProp(Prop, Entity)}
 * to combine two-phase init + registry registration in a single call.
 */
public abstract class PropFactory extends SceneObjectFactory {

    protected PropFactory(@NotNull String name, @NotNull SceneObjectRegistry registry) {
        super(name, registry);
    }

    /**
     * Initializes {@code prop} with {@code entity} and registers it.
     * Prefer this over calling {@link #spawn(me.mykindos.betterpvp.core.scene.SceneObject, Entity)}
     * directly in prop factory subclasses, as it communicates intent clearly.
     */
    public <T extends Prop> T spawnProp(@NotNull T prop, @NotNull Entity entity) {
        return spawn(prop, entity);
    }
}
