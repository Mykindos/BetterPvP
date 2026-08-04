package me.mykindos.betterpvp.clans.world;

import lombok.Value;
import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Function;

/**
 * An uninitialized {@link SceneObject} paired with <em>where</em> it lives and <em>how</em> to build its backing
 * entity. Returned by {@link WorldContent#sceneObjects} so
 * {@link me.mykindos.betterpvp.clans.world.content.WorldContentService} can register it as a chunk-managed object: the
 * object stays dormant until its anchor chunk's entities load, then {@code entityFactory} spawns its body, and it is
 * re-spawned across chunk cycles so it survives players leaving and returning to render range.
 */
@Value
public class SceneSpawn {

    SceneObject object;
    Location anchor;
    Function<Location, Entity> entityFactory;
}
