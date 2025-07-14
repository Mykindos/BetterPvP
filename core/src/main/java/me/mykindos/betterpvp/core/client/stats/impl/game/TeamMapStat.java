package me.mykindos.betterpvp.core.client.stats.impl.game;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import org.jetbrains.annotations.NotNull;

@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public abstract class TeamMapStat implements IBuildableStat {
    public static String NONE_TEAM_NAME = "NONE";
    @NotNull
    @Builder.Default
    protected String mapName = "";
    @NotNull
    @Builder.Default
    protected String teamName = "";
}
