package me.mykindos.betterpvp.game.guice.platform;

import com.google.inject.AbstractModule;
import lombok.CustomLog;

/**
 * Mineplex implementation of platform-specific bindings
 */
@CustomLog
public class MineplexPlatformProvider extends AbstractModule implements PlatformProvider {

    @Override
    protected void configure() {
        // Add Mineplex-specific bindings here
        // Example: bind(PunishmentModule.class).to(PunishmentModule.class);
        log.info("Configured Mineplex platform bindings").submit();
    }

    @Override
    public String getPlatformName() {
        return "Mineplex";
    }
}