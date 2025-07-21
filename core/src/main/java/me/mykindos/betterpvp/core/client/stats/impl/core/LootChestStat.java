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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class LootChestStat implements IBuildableStat {
    public static final String PREFIX = "LOOT_CHEST";

    private static StringBuilderParser<LootChestStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                LootChestStat::parsePrefix,
                    LootChestStat::parseSource,
                    LootChestStat::parseItem
            )
    );

    public static LootChestStat fromString(String string) {
        return parser.parse(LootChestStat.builder(), string).build();
    }

    private static LootChestStatBuilder parsePrefix(LootChestStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static LootChestStatBuilder parseSource(LootChestStatBuilder builder, String input) {
        return builder.source(input);
    }

    private static LootChestStatBuilder parseItem(LootChestStatBuilder builder, String input) {
        return builder.item(input);
    }

    @NotNull
    private String source;

    @Nullable
    private String item;

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        LootChestStat other = fromString(statName);
        this.source = other.source;
        this.item = other.item;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
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
                        source,
                        item
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

}
