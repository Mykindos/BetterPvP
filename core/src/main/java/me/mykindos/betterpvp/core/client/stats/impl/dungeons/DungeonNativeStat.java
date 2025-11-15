package me.mykindos.betterpvp.core.client.stats.impl.dungeons;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DungeonNativeStat extends DungeonStat implements IBuildableStat {
    public static final String TYPE = "DUNGEON_NATIVE";

    public static DungeonNativeStat fromData(String statType, JSONObject data) {
        DungeonNativeStat.DungeonNativeStatBuilder<?, ?> builder = builder();
        Preconditions.checkArgument(statType.equals(TYPE));
        builder.action(Action.valueOf(data.getString("action")));
        builder.dungeonName(data.getString("dungeonName"));
        return builder.build();
    }

    @NotNull
    private Action action;

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
        DungeonNativeStat other = fromData(statType, data);
        this.action = other.action;
        this.dungeonName = other.dungeonName;
        return this;
    }

    private boolean filterActionStat(Map.Entry<IStat, Long> entry) {
        final DungeonNativeStat stat = (DungeonNativeStat) entry.getKey();
        return action.equals(stat.action);
    }

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Long getStat(StatContainer statContainer, String periodKey) {
        if (Strings.isNullOrEmpty(dungeonName)) {
            return this.getFilteredStat(statContainer, periodKey, this::filterActionStat);
        }
        return statContainer.getProperty(periodKey, this);
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
        return Objects.requireNonNull(super.getJsonData())
                .putOnce("action", action.name());
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
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return !Strings.isNullOrEmpty(dungeonName);
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof DungeonNativeStat other)) return false;
        if (!Strings.isNullOrEmpty(dungeonName) && !dungeonName.equals(other.dungeonName)) return false;
        return action.equals(other.action);
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return DungeonNativeStat.builder().action(action).build();
    }

    public enum Action {
        ENTER,
        WIN,
        LOSS,
        //todo implement
        BOSS_KILL,
    }
}
