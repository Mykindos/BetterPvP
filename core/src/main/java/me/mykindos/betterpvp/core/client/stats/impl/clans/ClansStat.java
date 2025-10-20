package me.mykindos.betterpvp.core.client.stats.impl.clans;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@NoArgsConstructor
@Getter
public abstract class ClansStat implements IBuildableStat {
    public static final String NO_CLAN_NAME = "No";
    public static final String NO_ID_REPLACE = "No";
    @NotNull
    @Builder.Default
    protected String clanName = "";

    @Nullable("When not in a Clan")
    protected UUID clanId;
}
