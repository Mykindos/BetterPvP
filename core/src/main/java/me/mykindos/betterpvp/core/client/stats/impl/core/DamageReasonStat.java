package me.mykindos.betterpvp.core.client.stats.impl.core;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Type;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Builder
@EqualsAndHashCode
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DamageReasonStat implements IBuildableStat {
    public static String PREFIX = "DAMAGE_REASON";

    private static StringBuilderParser<DamageReasonStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    DamageReasonStat::parsePrefix,
                    DamageReasonStat::parseRelation,
                    DamageReasonStat::parseType,
                    DamageReasonStat::parseDamageCause
            )
    );

    public static DamageReasonStat fromString(String statName) {
        return parser.parse(DamageReasonStat.builder(), statName).build();
    }

    private static DamageReasonStatBuilder parsePrefix(DamageReasonStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static DamageReasonStatBuilder parseRelation(DamageReasonStatBuilder builder, String input) {
        return builder.relation(Relation.valueOf(input));
    }

    private static DamageReasonStatBuilder parseType(DamageReasonStatBuilder builder, String input) {
        return builder.type(Type.valueOf(input));
    }

    private static DamageReasonStatBuilder parseDamageCause(DamageReasonStatBuilder builder, String input) {
        return builder.damageCause(EntityDamageEvent.DamageCause.valueOf(input));
    }

    private static DamageReasonStatBuilder parseReason(DamageReasonStatBuilder builder, String input) {
        return builder.reason(input);
    }


    @NotNull
    private Relation relation;
    @NotNull
    private Type type;
    @NotNull
    private EntityDamageEvent.DamageCause damageCause;
    @NotNull
    private String reason;


    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param period
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String period) {
        return statContainer.getProperty(getStatName(), period);
    }

    @Override
    public String getStatName() {
        return parser.asString(List.of(
                PREFIX,
                relation.name(),
                damageCause == null ? "" : damageCause.name())
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
        return getStatName().startsWith(statName);
    }

    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        DamageReasonStat other = fromString(statName);
        this.relation = other.relation;
        this.damageCause = other.damageCause;
        this.reason = other.reason;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
