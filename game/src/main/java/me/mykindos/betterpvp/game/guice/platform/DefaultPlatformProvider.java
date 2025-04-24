package me.mykindos.betterpvp.game.guice.platform;

import com.google.inject.AbstractModule;
import lombok.CustomLog;

/**
 * Default implementation of platform-specific bindings
 */
@CustomLog
public class DefaultPlatformProvider extends AbstractModule implements PlatformProvider {

    @Override
    protected void configure() {
        // Add default bindings here
        // bind(PunishmentModule.class).to(FallbackPunishmentModule.class);
        log.info("Configured default platform bindings").submit();
    }

    @Override
    public String getPlatformName() {
        return "BetterPvP";
    }
}