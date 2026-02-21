package me.mykindos.betterpvp.core.client.stats.impl.dungeons;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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

/**
 * A stat that wraps around another {@link IStat} that adds game, team, and map context
 * if the stat is in a Champions Game
 */
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DungeonWrapperStat extends DungeonStat implements IWrapperStat {

    public static final String TYPE = "DUNGEON_WRAPPER";

    private static final StatBuilder statBuilder = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(StatBuilder.class);

    public static DungeonWrapperStat fromData(String statType, JSONObject data) {
        DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?> builder = builder();
        Preconditions.checkArgument(statType.equals(TYPE));
        builder.dungeonName(data.getString("dungeonName"));
        JSONObject wrappedData = data.getJSONObject("wrappedStat");
        builder.wrappedStat(statBuilder.getStatForStatData(wrappedData.getString("statType"), wrappedData));
        return builder.build();
    }

    @NotNull
    private IStat wrappedStat;

    private boolean filterDungeonStat(Map.Entry<IStat, Long> entry) {
        final DungeonWrapperStat stat = (DungeonWrapperStat) entry.getKey();
        return dungeonName.equals(stat.dungeonName) && wrappedStat.containsStat(stat.wrappedStat);
    }

    private boolean filterWrappedStat (Map.Entry<IStat, Long> entry) {
        final DungeonWrapperStat stat = (DungeonWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat.wrappedStat);
    }

    //todo figure out wtf we do with this
    //todo what happens if the wrapped stat is not savable?
    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        if (Strings.isNullOrEmpty(dungeonName)) {
            return this.getFilteredStat(statContainer, type, period, this::filterDungeonStat);
        }
        return this.getFilteredStat(statContainer, type, period, this::filterWrappedStat);
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
        return !Strings.isNullOrEmpty(dungeonName) &&
                wrappedStat.isSavable();
    }

    @Override
    public boolean containsStat(final IStat otherStat) {
        if (!(otherStat instanceof DungeonWrapperStat other)) return false;
        if (!Strings.isNullOrEmpty(dungeonName) && !dungeonName.equals(other.dungeonName)) return false;
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
        return DungeonWrapperStat.builder().wrappedStat(wrappedStat.getGenericStat()).build();
    }
    @Override
    public @Nullable JSONObject getJsonData() {
        return Objects.requireNonNull(super.getJsonData())
                .put("wrappedStat", (wrappedStat.getJsonData() == null ? new JSONObject() : wrappedStat.getJsonData())
                        .putOnce("statType", wrappedStat.getStatType())
                );
    }

    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        final DungeonWrapperStat other = fromData(statType, data);
        this.dungeonName = other.dungeonName;
        this.wrappedStat = other.wrappedStat;
        return this;
    }

}
