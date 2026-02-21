package me.mykindos.betterpvp.core.client.stats.impl.clans;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.StatBuilder;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.IWrapperStat;
import me.mykindos.betterpvp.core.server.Period;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Getter
@CustomLog
public class ClanWrapperStat extends ClansStat implements IWrapperStat {
    public static final String TYPE = "CLANS_WRAPPER";

    private static final StatBuilder statBuilder = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(StatBuilder.class);

    public ClanWrapperStat fromData(String statType, JSONObject data) {
        ClanWrapperStat.ClanWrapperStatBuilder<?, ?> builder = builder();
        Preconditions.checkArgument(statType.equals(TYPE));
        builder.clanName(data.getString("clanName"));
        builder.clanId(data.optLongObject("clanName", null));
        JSONObject wrappedData = data.getJSONObject("wrappedStat");
        builder.wrappedStat(statBuilder.getStatForStatData(wrappedData.getString("statType"), wrappedData));
        return builder.build();
    }

    @NotNull
    private IStat wrappedStat;

    private boolean filterClanStat(Map.Entry<IStat, Long> entry) {
        final ClanWrapperStat stat = (ClanWrapperStat) entry.getKey();
        return clanName.equals(stat.clanName) &&
                Objects.equals(clanId, stat.clanId) &&
                wrappedStat.containsStat(stat.wrappedStat);
    }

    private boolean filterWrappedStat (Map.Entry<IStat, Long> entry) {
        final ClanWrapperStat stat = (ClanWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat.wrappedStat);
    }


    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statType the statname
     * @param data
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        final ClanWrapperStat other = fromData(statType, data);
        this.clanName = other.clanName;
        this.clanId = other.clanId;
        this.wrappedStat = other.wrappedStat;
        return this;
    }

    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        //clanName being empty means it is for all Clans
        if (Strings.isNullOrEmpty(clanName)) {
            return this.getFilteredStat(statContainer, type, period, this::filterWrappedStat);
        }

        return this.getFilteredStat(statContainer, type, period, this::filterClanStat);
    }

    @Override
    public @NotNull String getStatType() {
        return TYPE;
    }

    /**
     * Get the simple name of this stat, without qualifications (if present)
     * <p>
     * i.e. Time Played, Flags Captured
     *
     * @return the simple name
     */
    @Override
    public String getSimpleName() {
        return wrappedStat.getSimpleName();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return !Strings.isNullOrEmpty(clanName) && wrappedStat.isSavable();
    }

    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof ClanWrapperStat other)) return false;
        if (!Strings.isNullOrEmpty(clanName) && !clanName.equals(other.clanName)) return false;
        //empty clanname means this is a generic stat, clanId equal does not matter
        if (!Strings.isNullOrEmpty(clanName) && !Objects.equals(clanId, other.clanId)) return false;
        return wrappedStat.containsStat(other.wrappedStat);
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return ClanWrapperStat.builder().wrappedStat(wrappedStat.getGenericStat()).build();
    }

    @Override
    public @Nullable JSONObject getJsonData() {
        return Objects.requireNonNull(super.getJsonData())
                .put("wrappedStat", (wrappedStat.getJsonData() == null ? new JSONObject() : wrappedStat.getJsonData())
                        .putOnce("statType", wrappedStat.getStatType())
                );
    }

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
        return getClanInformation() + wrappedStat.getQualifiedName();
    }
}
