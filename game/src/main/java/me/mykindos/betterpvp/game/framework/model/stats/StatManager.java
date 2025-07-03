package me.mykindos.betterpvp.game.framework.model.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.MapStat;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;

import java.util.UUID;

@Singleton
public class StatManager {
    private final ServerController serverController;
    private final MapManager mapManager;
    private final ClientManager clientManager;

    @Inject
    public StatManager(ServerController serverController, MapManager mapManager, ClientManager clientManager) {
        this.serverController = serverController;
        this.mapManager = mapManager;
        this.clientManager = clientManager;
    }

    /**
     * Increments the stat and adds the current map
     * @param id
     * @param statBuilder
     * @param amount
     */
    public void incrementStat(UUID id, MapStat.MapStatBuilder<?, ?> statBuilder, double amount) {
        final String mapName = serverController.getCurrentState().isInLobby() ? mapManager.getWaitingLobby().getMetadata().getName() : mapManager.getCurrentMap().getMetadata().getName();
        IStat finalStat = statBuilder.mapName(mapName).build();
        clientManager.incrementStatOffline(id, finalStat, amount);
    }
}
