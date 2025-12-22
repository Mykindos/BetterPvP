package me.mykindos.betterpvp.game.framework.model.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapWrapperStat;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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


    /**
     * Adds the {@link GameTeamMapStat#getGameName() gameName}, {@link GameTeamMapStat#getTeamName() teamName},
     * {@link GameTeamMapStat#getMapName() mapName}, {@link GameTeamMapStat#getGameId() gameId} from the {@link ServerController#getCurrentGameInfo() current game info}
     * @param id the {@link Player#getUniqueId() player's uuid}
     * @param statBuilder the {@link GameTeamMapStat.GameTeamMapStatBuilder}
     * @return the {@link GameTeamMapStat.GameTeamMapStatBuilder builder} with the filled in elements
     */
    public GameTeamMapStat.GameTeamMapStatBuilder<?, ?> addGameMapStatElements(@NotNull UUID id, @NotNull GameTeamMapStat.GameTeamMapStatBuilder<?, ?> statBuilder) {
        final GameInfo gameInfo = serverController.getCurrentGameInfo();
        final String gameName = gameInfo.getGameName();
        final String teamName = gameInfo.getPlayerTeams().get(id) == null ? GameTeamMapStat.NONE_TEAM_NAME : gameInfo.getPlayerTeams().get(id);
        final String mapName = gameInfo.getMapName();
        final long gameId = gameInfo.getId();
        statBuilder.gameName(gameName);
        statBuilder.teamName(teamName);
        statBuilder.mapName(mapName);
        return statBuilder.gameId(gameId);
    }

    /**
     * Increments the stat and adds the current information
     * @param id the {@link Player#getUniqueId() player's uuid}
     * @param statBuilder the {@link GameTeamMapStat.GameTeamMapStatBuilder} with a filled in {@link GameTeamMapNativeStat#getAction() action}
     *                    or filled in {@link GameTeamMapWrapperStat#getWrappedStat() wrappedStat}
     * @param amount the amount to increment this stat by
     * @see StatManager#addGameMapStatElements(UUID, GameTeamMapStat.GameTeamMapStatBuilder)
     */
    public void incrementGameMapStat(UUID id, GameTeamMapStat.GameTeamMapStatBuilder<?, ?> statBuilder, long amount) {
        final IStat finalStat = addGameMapStatElements(id, statBuilder).build();
        clientManager.incrementStatOffline(id, finalStat, amount);
    }

}
