package me.mykindos.betterpvp.core.client.stats.impl.clans;

import joptsimple.internal.Strings;
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
        final StringBuilder stringBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(clanName)) {
            stringBuilder.append(clanName)
                    .append(" ");
        }
        return stringBuilder.append(getSimpleName()).toString();
    }
}
