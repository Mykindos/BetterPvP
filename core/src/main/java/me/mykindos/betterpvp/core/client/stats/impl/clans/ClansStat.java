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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@NoArgsConstructor
@Getter
public abstract class ClansStat implements IBuildableStat {
    public static final String NO_CLAN_NAME = "No";
    public static final String NO_ID_REPLACE = "No";
    /**
     * The Clan Name, empty if getting stat of all Clans
     */
    @NotNull
    @Builder.Default
    protected String clanName = "";

    @Nullable("When not in a Clan")
    protected Long clanId;


    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return new JSONObject()
                .putOpt("clanName", clanName)
                .putOpt("clanId", clanId);
    }

    protected String getClanInformation() {
        final StringBuilder stringBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(clanName)) {
            if (clanName.equals(NO_CLAN_NAME)) {
                stringBuilder.append("No Clan").append(" ");
            } else {
                stringBuilder.append(clanName)
                        .append(" ");
            }

        }
        return stringBuilder.toString();
    }
}
