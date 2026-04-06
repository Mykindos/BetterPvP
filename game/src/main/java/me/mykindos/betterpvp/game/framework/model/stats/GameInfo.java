package me.mykindos.betterpvp.game.framework.model.stats;

import lombok.Data;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import static me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator.ID_GENERATOR;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GameInfo {
    public static final String LOBBY_GAME_NAME = GameTeamMapStat.LOBBY_GAME_NAME;
    public static final String NONE_TEAM_NAME = GameTeamMapStat.NONE_TEAM_NAME;


    private final long id;
    private final String gameName;
    private final String mapName;
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();

    public GameInfo(String mapName, String gameName) {
        this.id = ID_GENERATOR.nextId();
        this.mapName = mapName;
        this.gameName = gameName;
    }
}
