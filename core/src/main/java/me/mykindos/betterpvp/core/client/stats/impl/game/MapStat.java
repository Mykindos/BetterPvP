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
public abstract class MapStat implements IBuildableStat {
    @NotNull
    @Builder.Default
    protected String mapName = "";
}
