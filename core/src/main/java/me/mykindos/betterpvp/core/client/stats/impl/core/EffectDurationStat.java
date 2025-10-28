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
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Relation;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class EffectDurationStat implements IBuildableStat {
    public static final String PREFIX = "EFFECT_DURATION";

    private static StringBuilderParser<EffectDurationStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    EffectDurationStat::parsePrefix,
                    EffectDurationStat::parseRelation,
                    EffectDurationStat::parseEffectType,
                    EffectDurationStat::parseEffectName
            )
    );

    public static EffectDurationStat fromString(String string) {
        return parser.parse(EffectDurationStat.builder(), string).build();
    }

    @NotNull
    private Relation relation;
    @NotNull
    private String effectType;
    @Nullable("When getting composite of effectType")
    @Builder.Default
    private String effectName = "";

    private static EffectDurationStatBuilder parsePrefix(EffectDurationStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static EffectDurationStatBuilder parseRelation(EffectDurationStatBuilder builder, String input) {
        return builder.relation(Relation.valueOf(input));
    }

    private static EffectDurationStatBuilder parseEffectType(EffectDurationStatBuilder builder, String input) {
        return builder.effectType(input);
    }

    private static EffectDurationStatBuilder parseEffectName(EffectDurationStatBuilder builder, String input) {
        return builder.effectName(input);
    }

    private boolean filterEffectTypeStat(Map.Entry<IStat, Double> entry) {
        EffectDurationStat stat = (EffectDurationStat) entry.getKey();
        return effectType.equals(stat.effectType);
    }

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String periodKey) {
        if (effectName == null) {
            return getFilteredStat(statContainer, periodKey, this::filterEffectTypeStat);
        }
        return statContainer.getProperty(periodKey, this);
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        relation.name(),
                        effectType,
                        effectName
                )
        );
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
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        return statName.startsWith(getStatName());
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
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        EffectDurationStat other = fromString(statName);
        relation = other.getRelation();
        effectType = other.getEffectType();
        effectName = other.getEffectName();
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
