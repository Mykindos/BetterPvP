package me.mykindos.betterpvp.game.guice.platform;

import com.google.inject.Module;

/**
 * Interface for platform-specific bindings
 */
public interface PlatformProvider extends Module {
    /**
     * @return human-readable name of the platform
     */
    String getPlatformName();
}