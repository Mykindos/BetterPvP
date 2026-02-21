package me.mykindos.betterpvp.core.client.stats.impl.core;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Type;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseRegistry;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

@Builder
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DamageStat implements IBuildableStat {
    public static final String STAT_TYPE = "DAMAGE";
    private static final DamageCauseRegistry damageCauseRegistry = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(DamageCauseRegistry.class);

    public static DamageStat fromData(String statType, JSONObject data) {
        DamageStat.DamageStatBuilder builder = builder();
        Preconditions.checkArgument(statType.equals(STAT_TYPE));
        builder.relation(Relation.valueOf(data.getString("relation")));
        builder.type(Type.valueOf(data.getString("type")));
        builder.damageCause(damageCauseRegistry.get(data.getString("damageCause")));
        return builder.build();
    }


    @NotNull
    private Relation relation;
    @NotNull
    private Type type;
    @Nullable
    private DamageCause damageCause;


    private boolean filterDamageCause(Map.Entry<IStat, Long> entry) {
        DamageStat other = (DamageStat) entry.getKey();
        return relation.equals(other.relation) && type.equals(other.type);
    }

    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        if (damageCause == null) {
            return getFilteredStat(statContainer, type, period, this::filterDamageCause);
        }
        return statContainer.getProperty(type, period, this);
    }

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    public @NotNull StatValueType getStatValueType() {
        return StatValueType.DOUBLE;
    }

    @Override
    public @NotNull String getStatType() {
        return STAT_TYPE;
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return new JSONObject()
                .putOnce("relation", relation.name())
                .putOnce("type", type.name())
                .putOnce("damageCause", Objects.requireNonNull(damageCause).getName());
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
        StringBuilder stringBuilder = new StringBuilder()
                .append(UtilFormat.cleanString(relation.name()))
                .append(" ")
                .append(UtilFormat.cleanString(type.name()));
        if (damageCause != null) {
            stringBuilder.append(" ")
                    .append(UtilFormat.cleanString(damageCause.getName()));
        }

        return stringBuilder.toString();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return damageCause != null;
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof DamageStat other)) return false;
        if ((relation != other.relation) || (type != other.type)) return false;
        return (damageCause != null && damageCause.equals(other.damageCause));
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return DamageStat.builder().relation(relation).type(type).build();
    }

    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        DamageStat other = fromData(statType, data);
        this.relation = other.relation;
        this.type = other.type;
        this.damageCause = other.damageCause;
        return this;
    }

}
