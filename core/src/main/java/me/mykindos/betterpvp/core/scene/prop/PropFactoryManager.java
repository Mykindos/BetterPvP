package me.mykindos.betterpvp.core.scene.prop;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

/**
 * Registry of all active {@link PropFactory} instances, keyed by their namespace (e.g. {@code "hub"}).
 * <p>
 * Used by prop-spawn commands to look up the correct factory for a given namespace + type string.
 */
@Singleton
public class PropFactoryManager extends Manager<String, PropFactory> {
}
