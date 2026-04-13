package me.mykindos.betterpvp.core.scene;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

/**
 * Stores all registered {@link SceneObjectFactory} instances, keyed by factory name.
 * <p>
 * Module-specific sub-managers (e.g. {@code NPCFactoryManager}) may extend this class
 * to expose typed views of a specific factory subtype.
 */
@Singleton
public class SceneObjectFactoryManager extends Manager<String, SceneObjectFactory> {
}
