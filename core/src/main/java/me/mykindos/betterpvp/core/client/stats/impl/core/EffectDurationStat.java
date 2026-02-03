package me.mykindos.betterpvp.core.client.stats.impl.core;

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
import me.mykindos.betterpvp.core.client.stats.impl.utility.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
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
public class EffectDurationStat implements IBuildableStat {
    public static final String TYPE = "EFFECT_DURATION";

    public static EffectDurationStat fromData(String statType, JSONObject data) {
        EffectDurationStat.EffectDurationStatBuilder builder = builder();
        Preconditions.checkArgument(statType.equals(TYPE));
        builder.relation(Relation.valueOf(data.getString("relation")));
        builder.effectType(data.getString("effectType"));
        builder.effectName(data.optString("effectName"));
        return builder.build();
    }

    @NotNull
    private Relation relation;
    @NotNull
    private String effectType;
    @Nullable("When getting composite of effectType")
    @Builder.Default
    private String effectName = "";

    private boolean filterEffectTypeStat(Map.Entry<IStat, Long> entry) {
        EffectDurationStat stat = (EffectDurationStat) entry.getKey();
        return effectType.equals(stat.effectType);
    }

    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        if (effectName == null) {
            return getFilteredStat(statContainer, type, period, this::filterEffectTypeStat);
        }
        return statContainer.getProperty(type, period, this);
    }

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    public StatValueType getStatValueType() {
        return StatValueType.DURATION;
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
                .putOnce("relation", relation.name())
                .putOnce("effectType", effectType)
                .putOnce("effectName", effectName);
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
        final StringBuilder stringBuilder = new StringBuilder(UtilFormat.cleanString(relation.name()));
        stringBuilder.append(" ")
                .append(UtilFormat.cleanString(effectType));
        if (!Strings.isNullOrEmpty(effectName)) {
            stringBuilder.append(" ")
                    .append(effectName);
        }
        return stringBuilder.toString();
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
        return IBuildableStat.super.getQualifiedName();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return effectName != null;
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof EffectDurationStat other)) return false;
        if ((relation != other.relation) || (!effectType.equals(other.effectType))) return false;
        return (!Strings.isNullOrEmpty(effectName) && effectName.equals(other.effectName));
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return EffectDurationStat.builder()
                .relation(relation)
                .effectType(effectType)
                .build();
    }

    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        EffectDurationStat other = fromData(statType, data);
        relation = other.getRelation();
        effectType = other.getEffectType();
        effectName = other.getEffectName();
        return this;
    }
}
