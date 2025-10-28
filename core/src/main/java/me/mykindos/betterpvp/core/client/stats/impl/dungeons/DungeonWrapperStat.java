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
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.IWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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

    public static final String PREFIX = "DUNGEON_WRAPPER";

    private static final StatBuilder statBuilder = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(StatBuilder.class);

    private static final StringBuilderParser<DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            "#",
            "##",
            List.of(
                    DungeonWrapperStat::parsePrefix,
                    DungeonWrapperStat::parseName
            ),
            List.of(
                    DungeonWrapperStat::parseWrappedStat
            )
    );

    /**
     * Constructs the given String into a Stat
     * @param string the stringified stat
     * @return this stat
     * @throws IllegalArgumentException if this string does not represent this Stat
     */
    public static DungeonWrapperStat fromString(String string) {
        return parser.parse(DungeonWrapperStat.builder(), string).build();
    }

    @NotNull
    private IStat wrappedStat;

    private static DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?> parsePrefix(DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }
    private static DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?> parseWrappedStat(DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?> builder, String input) {
        final IStat wrappedStat = statBuilder.getStatForStatName(input);
        if (wrappedStat instanceof DungeonStat) throw new IllegalArgumentException("Wrapped stat cannot also be a DungeonStat");
        return builder.wrappedStat(wrappedStat);
    }

    private static DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?> parseName(DungeonWrapperStat.DungeonWrapperStatBuilder<?, ?> builder, String input) {
        return builder.dungeonName(input);
    }

    private boolean filterDungeonStat(Map.Entry<IStat, Double> entry) {
        final DungeonWrapperStat stat = (DungeonWrapperStat) entry.getKey();
        return dungeonName.equals(stat.dungeonName) && wrappedStat.containsStat(stat.wrappedStat);
    }

    private boolean filterWrappedStat (Map.Entry<IStat, Double> entry) {
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
    public Double getStat(StatContainer statContainer, String periodKey) {
        if (joptsimple.internal.Strings.isNullOrEmpty(dungeonName)) {
            return this.getFilteredStat(statContainer, periodKey, this::filterDungeonStat);
        }
        return this.getFilteredStat(statContainer, periodKey, this::filterWrappedStat);
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        dungeonName
                ),
                List.of(
                        wrappedStat.getStatName()
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

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    //TODO, this might not be true anymore
    public boolean containsStat(final String statName) {
        try {
            final DungeonWrapperStat other = fromString(statName);
            //all filled fields must equal all the other filled fields
            //TODO check the logic here
            if (!Strings.isNullOrEmpty(dungeonName) && !dungeonName.equals(other.dungeonName)) return false;

            return wrappedStat.containsStat(other.wrappedStat);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
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
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        final DungeonWrapperStat other = fromString(statName);
        this.dungeonName = other.dungeonName;
        this.wrappedStat = other.wrappedStat;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
