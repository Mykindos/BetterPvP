package me.mykindos.betterpvp.clans.mineplex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.game.BuiltInGameState;
import com.mineplex.studio.sdk.modules.game.MineplexGame;
import com.mineplex.studio.sdk.modules.game.MineplexGameModule;
import com.mineplex.studio.sdk.modules.game.event.PostMineplexGameStateChangeEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.BetterPVPMineplexGame;
import org.bukkit.Bukkit;

@Singleton
@PluginAdapter("StudioEngine")
@CustomLog
public class MineplexAdapter {

    @Inject
    public MineplexAdapter(Clans clans) {
        UtilServer.runTaskLater(clans, this::registerGame, 1L);
        log.info("Loaded MineplexAdapter").submit();
    }

    private void registerGame() {
        final MineplexGameModule gameModule = MineplexModuleManager.getRegisteredModule(
                MineplexGameModule.class);

        MineplexGame game = new BetterPVPMineplexGame();
        gameModule.setCurrentGame(game);
        game.setGameState(BuiltInGameState.STARTED);

        Bukkit.getPluginManager().callEvent(new PostMineplexGameStateChangeEvent(game, BuiltInGameState.STARTED, BuiltInGameState.STARTED));
    }

}
