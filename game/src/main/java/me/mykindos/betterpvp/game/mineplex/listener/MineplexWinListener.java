package me.mykindos.betterpvp.game.mineplex.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.mineplex.MineplexMessage;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageSentEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.entity.Player;

import java.util.Optional;

@CustomLog
@Singleton
public class MineplexWinListener {

    private final GamePlugin plugin;
    private final ClientManager clientManager;
    private final ServerController serverController;

    @Inject
    public MineplexWinListener(GamePlugin plugin, ServerController serverController, ClientManager clientManager) {
        this.plugin = plugin;
        this.clientManager = clientManager;
        this.serverController = serverController;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        serverController.getStateMachine().addEnterHandler(GameState.ENDING, this::awardWinners);
    }

    private void awardWinners(GameState gameState) {
        for (Audience winner : serverController.getCurrentGame().getWinners()) {
            if (winner instanceof Player player) {
                awardWinner(player);
            } else if (winner instanceof ForwardingAudience forwardingAudience) {
                forwardingAudience.forEachAudience(audience -> {
                    if (audience instanceof Player player) {
                        awardWinner(player);
                    }
                });
            }
        }
    }

    private void awardWinner(Player player) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        final Optional<Integer> winsOpt = gamer.getProperty("CHAMPIONS_WINS");
        int wins = winsOpt.orElse(0) + 1;
        gamer.saveProperty("CHAMPIONS_WINS", wins);

        if (wins > 0 && wins % 10 == 0) {
            final MineplexMessageSentEvent event = new MineplexMessageSentEvent("BetterPvP", MineplexMessage.builder()
                    .channel("ChampionsWinsReward")
                    .message(String.valueOf(wins))
                    .metadata("uuid", player.getUniqueId().toString())
                    .build());
            UtilServer.callEventAsync(plugin, event);
        }
    }
}
