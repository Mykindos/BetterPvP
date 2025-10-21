package me.mykindos.betterpvp.core.client.stats.impl.game;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import org.jetbrains.annotations.NotNull;

@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@NoArgsConstructor
@Getter
public abstract class GameTeamMapStat implements IBuildableStat {
    public static final String NONE_TEAM_NAME = "NONE";
    public static final String SPECTATOR_TEAM_NAME = "SPECTATOR";
    public static final String LOBBY_GAME_NAME = "Lobby";
    @NotNull
    @Builder.Default
    protected String gameName = "";
    @NotNull
    @Builder.Default
    protected String mapName = "";
    @NotNull
    @Builder.Default
    protected String teamName = "";
}
