package me.mykindos.betterpvp.clans.world;

import lombok.Value;
import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.entity.Entity;

/**
 * An uninitialized {@link SceneObject} paired with the backing {@link Entity} it should bind to. Returned by
 * {@link WorldContent#sceneObjects} so {@link ContinentSceneLoader} can init, register, and track it through the
 * standard {@link me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader} lifecycle.
 */
@Value
public class SceneSpawn {

    SceneObject object;
    Entity entity;
}
