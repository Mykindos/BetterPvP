package me.mykindos.betterpvp.game.guice.platform;

import com.google.inject.AbstractModule;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.game.MineplexGame;
import com.mineplex.studio.sdk.modules.game.MineplexGameModule;
import lombok.CustomLog;
import me.mykindos.betterpvp.game.mineplex.ChampionsGame;
import me.mykindos.betterpvp.game.mineplex.ChampionsGameCycle;

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
        bind(MineplexGame.class).to(ChampionsGame.class).asEagerSingleton();
    }

    @Override
    public String getPlatformName() {
        return "Mineplex";
    }
}