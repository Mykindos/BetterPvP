package me.mykindos.betterpvp.core.client.stats.impl.dungeons;

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
public abstract class DungeonStat implements IBuildableStat {


    @NotNull
    @Builder.Default
    protected String dungeonName = "";
}
