package me.mykindos.betterpvp.core.client.stats.impl.clans;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
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
import java.util.UUID;

@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Getter
@CustomLog
public class ClanWrapperStat extends ClansStat implements IWrapperStat {
    public static final String PREFIX = "CLANS_WRAPPER";

    private static final StatBuilder statBuilder = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(StatBuilder.class);

    private static final StringBuilderParser<ClanWrapperStatBuilder<?, ?>> parser = new StringBuilderParser<>(
            //clans wrapper needs different delimiters than dungeons or events because it will wrap those stats too
            "!",
            "!!",
            List.of(
                    ClanWrapperStat::parsePrefix,
                    ClanWrapperStat::parseClanID,
                    ClanWrapperStat::parseClanName
            ),
            List.of(
                    ClanWrapperStat::parseWrappedStat
            )
    );

    /**
     * Constructs the given String into a Stat
     * @param string the stringified stat
     * @return this stat
     * @throws IllegalArgumentException if this string does not represent this Stat
     */
    public static ClanWrapperStat fromString(String string) {
        return parser.parse(ClanWrapperStat.builder(), string).build();
    }

    @NotNull
    private IStat wrappedStat;

    private static ClanWrapperStatBuilder<?, ?> parsePrefix(ClanWrapperStatBuilder<?, ?> builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }
    private static ClanWrapperStatBuilder<?, ?> parseWrappedStat(ClanWrapperStatBuilder<?, ?> builder, String input) {
        final IStat wrappedStat = statBuilder.getStatForStatName(input);
        if (wrappedStat instanceof ClansStat) throw new IllegalArgumentException("Wrapped stat cannot also be a ClansStat");
        return builder.wrappedStat(wrappedStat);
    }

    private static ClanWrapperStatBuilder<?, ?> parseClanName(ClanWrapperStatBuilder<?, ?> builder, String input) {
        return builder.clanName((input));
    }

    private static ClanWrapperStatBuilder<?, ?> parseClanID(ClanWrapperStatBuilder<?, ?> builder, String input) {
        try {
            return builder.clanId(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            return builder.clanId(null);
        }
    }

    private boolean filterClanStat(Map.Entry<IStat, Double> entry) {
        final ClanWrapperStat stat = (ClanWrapperStat) entry.getKey();
        if (clanId == null && stat.clanId != null) return false;
        return (clanId == null || clanId.equals(stat.clanId) && wrappedStat.containsStat(stat.wrappedStat));
    }

    private boolean filterWrappedStat (Map.Entry<IStat, Double> entry) {
        final ClanWrapperStat stat = (ClanWrapperStat) entry.getKey();
        return wrappedStat.containsStat(stat.wrappedStat);
    }


    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        final ClanWrapperStat other = fromString(statName);
        this.clanName = other.clanName;
        this.clanId = other.clanId;
        this.wrappedStat = other.wrappedStat;
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
     * @param periodKey
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String periodKey) {
        //clanName being empty means it is for all Clans
        if (Strings.isNullOrEmpty(clanName)) {
            return this.getFilteredStat(statContainer, periodKey, this::filterWrappedStat);
        }

        return this.getFilteredStat(statContainer, periodKey, this::filterClanStat);
    }

    @Override
    public String getStatName() {
        return parser.asString(
                List.of(
                        PREFIX,
                        clanId == null ? ClansStat.NO_ID_REPLACE : clanId.toString(),
                        clanName
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
        return !Strings.isNullOrEmpty(clanName) && wrappedStat.isSavable();
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        try {
            return containsStat(fromString(statName));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof ClanWrapperStat other)) return false;
        if (!Strings.isNullOrEmpty(clanName) && !clanName.equals(other.clanName)) return false;
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
        return ClanWrapperStat.builder().wrappedStat(wrappedStat.getGenericStat()).build();
    }
}
