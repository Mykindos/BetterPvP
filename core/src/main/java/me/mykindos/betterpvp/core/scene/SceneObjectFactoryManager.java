package me.mykindos.betterpvp.core.scene;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

/**
 * Stores all registered {@link SceneObjectFactory} instances, keyed by factory name.
 * <p>
 * There is a single factory type for every kind of scene object (props, NPCs, mobs, displays),
 * so one factory can spawn a mix of object kinds and they all live in this one manager. The
 * {@code /npc spawn <factory> <type>} command enumerates factories from here.
 */
@Singleton
public class SceneObjectFactoryManager extends Manager<String, SceneObjectFactory> {
}
