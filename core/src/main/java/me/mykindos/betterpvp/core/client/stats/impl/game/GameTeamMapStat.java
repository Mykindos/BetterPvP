package me.mykindos.betterpvp.core.client.stats.impl.game;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
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

    /**
     * Get the qualified name of the stat, if one exists.
     * Should usually end with the {@link IStat#getSimpleName()}
     * <p>
     * i.e. Domination Time Played, Capture the Flag CTF_Oakvale Flags Captured
     *
     * @return the qualified name
     */
    @Override
    public String getQualifiedName() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(gameName)) {
            stringBuilder.append(gameName);
            stringBuilder.append(" ");
        }
        if (!Strings.isNullOrEmpty(mapName)) {
            stringBuilder.append(mapName);
            stringBuilder.append(" ");
        }
        if (!Strings.isNullOrEmpty(teamName)) {
            stringBuilder.append(teamName);
            stringBuilder.append(" ");
        }

        return stringBuilder.append(getSimpleName()).toString();
    }
}
