package me.mykindos.betterpvp.game.framework.model.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.team.Team;

import java.util.UUID;

@Singleton
public class StatManager {
    private final ServerController serverController;
    private final MapManager mapManager;
    private final ClientManager clientManager;
    private final GameInfoRepository gameInfoRepository;

    @Inject
    public StatManager(ServerController serverController, MapManager mapManager, ClientManager clientManager, GameInfoRepository gameInfoRepository) {
        this.serverController = serverController;
        this.mapManager = mapManager;
        this.clientManager = clientManager;
        this.gameInfoRepository = gameInfoRepository;
    }

    public void save(GameInfo gameInfo) {
        gameInfoRepository.save(gameInfo);
    }

    public GameTeamMapStat.GameTeamMapStatBuilder<?, ?> addGameMapStatElements(UUID id, GameTeamMapStat.GameTeamMapStatBuilder<?, ?> statBuilder) {
        final String gameName = serverController.getCurrentState().isInLobby() ? GameTeamMapStat.LOBBY_GAME_NAME : serverController.getCurrentGame().getConfiguration().getName();
        statBuilder.gameName(gameName);
        if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame && !serverController.getCurrentState().isInLobby()) {
            final Team team = teamGame.getPlayerTeam(id);
            final String teamName = team == null ? GameTeamMapStat.SPECTATOR_TEAM_NAME : team.getProperties().name();
            statBuilder.teamName(teamName);
        } else {
            statBuilder.teamName(GameTeamMapStat.NONE_TEAM_NAME);
        }

        final String mapName = serverController.getCurrentState().isInLobby() ? mapManager.getWaitingLobby().getMetadata().getName() : mapManager.getCurrentMap().getMetadata().getName();
        return statBuilder.mapName(mapName);
    }

    /**
     * Increments the stat and adds the current map
     * @param id
     * @param statBuilder
     * @param amount
     */
    public void incrementGameMapStat(UUID id, GameTeamMapStat.GameTeamMapStatBuilder<?, ?> statBuilder, long amount) {
        IStat finalStat = addGameMapStatElements(id, statBuilder).build();
        clientManager.incrementStatOffline(id, finalStat, amount);
    }

}
