package me.mykindos.betterpvp.core.client.stats.impl;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Builder
@Getter
public class EffectDurationStat implements IStat {
    public static String PREFIX = "EFFECT_DURATION";

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
    @NotNull
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

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param period
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String period) {
        return statContainer.getProperty(period, getStatName());
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
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return true;
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        return getStatName().equals(statName);
    }

    public enum Relation {
        DEALT,
        RECEIVED
    }
}
