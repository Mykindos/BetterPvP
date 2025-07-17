package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import me.mykindos.betterpvp.game.impl.ctf.model.Flag;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles flag ticking
 */
@GameScoped
@CustomLog
public class FlagTicker implements Listener {

    private final GamePlugin plugin;
    private final ServerController serverController;
    private final GameController gameController;
    private BukkitTask flagTicker;

    @Inject
    public FlagTicker(GamePlugin plugin, ServerController serverController, GameController gameController) {
        this.plugin = plugin;
        this.serverController = serverController;
        this.gameController = gameController;
        scheduleTicker();
    }

    private void scheduleTicker() {
        flagTicker = new BukkitRunnable() {
            @Override
            public void run() {
                gameController.getFlags().values().forEach(Flag::tick);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        serverController.getStateMachine().addExitHandler(GameState.IN_GAME, oldState -> {
            log.info("Cancelled FlagTicker task").submit();
            flagTicker.cancel(); // Cancel the task when game ends
        });
    }

}
