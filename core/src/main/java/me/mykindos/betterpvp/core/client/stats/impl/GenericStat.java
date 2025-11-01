package me.mykindos.betterpvp.core.client.stats.impl;

import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
@EqualsAndHashCode()
@AllArgsConstructor
@CustomLog
public class GenericStat implements IStat {

    @NotNull
    private final IStat stat;

    private boolean filterStat(Map.Entry<IStat, Double> entry) {
        return containsStat(entry.getKey());
    }

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer the statContainer to source the value from
     * @param periodKey     the period to fetch from
     * @return the stat value represented by this stat
     */
    @Override
    public Double getStat(StatContainer statContainer, String periodKey) {
        return getFilteredStat(statContainer, periodKey, this::filterStat);
    }

    /**
     * Get the name that is stored in the DB
     *
     * @return
     */
    @Override
    public String getStatName() {
        return "GENERIC " + stat.getStatName();
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
        return stat.getSimpleName();
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
        return "Generic " + stat.getQualifiedName();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return false;
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        throw new NotImplementedException("string containsStat is deprecated");
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        while (true) {
            if (otherStat.containsStat(stat)) {
                return true;
            }
            if (otherStat instanceof final IWrapperStat wrapperStat) {
                otherStat = wrapperStat.getWrappedStat();
            } else {
                return false;
            }
        }
    }

    /**
     * Whether this stat can be wrapped by an {@link IWrapperStat}
     *
     * @return {@code true} if it can be wrapped, else {@code false}
     */
    @Override
    public boolean wrappingAllowed() {
        return false;
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return this;
    }
}
