package me.mykindos.betterpvp.core.client.stats.impl.core;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Type;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Builder
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DamageStat implements IBuildableStat {
    public static final String PREFIX = "DAMAGE";

    private static StringBuilderParser<DamageStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    DamageStat::parsePrefix,
                    DamageStat::parseRelation,
                    DamageStat::parseType,
                    DamageStat::parseDamageCause
            )
    );

    public static DamageStat fromString(String statName) {
        return parser.parse(DamageStat.builder(), statName).build();
    }

    private static DamageStatBuilder parsePrefix(DamageStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static DamageStatBuilder parseRelation(DamageStatBuilder builder, String input) {
        return builder.relation(Relation.valueOf(input));
    }

    private static DamageStatBuilder parseType(DamageStatBuilder builder, String input) {
        return builder.type(Type.valueOf(input));
    }

    private static DamageStatBuilder parseDamageCause(DamageStatBuilder builder, String input) {
        return builder.damageCause(EntityDamageEvent.DamageCause.valueOf(input));
    }

    //todo serialize from string
    @NotNull
    private Relation relation;
    @NotNull
    private Type type;
    @Nullable
    private EntityDamageEvent.DamageCause damageCause;


    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String periodKey) {
        if (damageCause == null) {
            return statContainer.getStats().getStatsOfPeriod(periodKey).entrySet().stream()
                    .filter(entry ->
                    entry.getKey().getStatName().startsWith(getStatName())
            ).mapToDouble(Map.Entry::getValue)
                    .sum();
        }
        return statContainer.getProperty(getStatName(), this);
    }

    @Override
    public String getStatName() {
        return parser.asString(List.of(
                PREFIX,
                relation.name(),
                type.name(),
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
        return damageCause != null;
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

    @Override
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        DamageStat other = fromString(statName);
        this.relation = other.relation;
        this.type = other.type;
        this.damageCause = other.damageCause;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
