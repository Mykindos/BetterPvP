package me.mykindos.betterpvp.core.client.stats.impl.events;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class BossStat implements IBuildableStat {
    public static final String TYPE = "EVENT_BOSS";

    public static BossStat fromData(String type, JSONObject data) {
        BossStat.BossStatBuilder builder = BossStat.builder();
        Preconditions.checkArgument(type.equals(TYPE));
        builder.action(Action.valueOf(data.getString("action")));
        builder.bossName(data.getString("bossName"));
        return builder.build();
    }

    @NotNull
    private Action action;

    @Nullable
    private String bossName;

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
        BossStat other = fromData(statType, data);
        this.action = other.action;
        this.bossName = other.bossName;
        return this;
    }

    private boolean filterActionStat(Map.Entry<IStat, Long> entry) {
        BossStat other = (BossStat) entry.getKey();
        return action.equals(other.action);
    }

    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        if (Strings.isNullOrEmpty(bossName)) {
            return getFilteredStat(statContainer, type, period, this::filterActionStat);
        }
        return statContainer.getProperty(type, period, this);
    }

    @Override
    public @NotNull String getStatType() {
        return TYPE;
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return new JSONObject()
                .putOnce("action", action.name())
                .putOnce("bossName", bossName);
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
        return UtilFormat.cleanString(action.name());
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
        StringBuilder stringBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(bossName)) {
            stringBuilder.append(bossName);
            stringBuilder.append(" ");
        }
        return stringBuilder.append(getSimpleName()).toString();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return !Strings.isNullOrEmpty(bossName);
    }

    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof BossStat other)) return false;
        if (!action.equals(other.action)) return false;
        if (!Strings.isNullOrEmpty(bossName) && !bossName.equals(other.bossName)) return false;
        return true;
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return BossStat.builder().action(action).build();
    }

    public enum Action {
        KILL,
    }
}
