package me.mykindos.betterpvp.game.guice.platform;

import com.google.inject.Module;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;

/**
 * Interface for platform-specific bindings
 */
public interface PlatformProvider extends Module, Lifecycled {
    /**
     * @return human-readable name of the platform
     */
    String getPlatformName();

    @Override
    default void setup() {}

    @Override
    default void tearDown() {}
}