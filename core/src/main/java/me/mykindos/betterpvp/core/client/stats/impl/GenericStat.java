package me.mykindos.betterpvp.core.client.stats.impl;

import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.server.Period;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;

@Getter
@EqualsAndHashCode()
@AllArgsConstructor
@CustomLog
public class GenericStat implements IStat {

    @NotNull
    private final IStat stat;

    private boolean filterStat(Map.Entry<IStat, Long> entry) {
        return containsStat(entry.getKey());
    }

    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        return getFilteredStat(statContainer, type, period, this::filterStat);
    }

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    public @NotNull StatValueType getStatValueType() {
        return stat.getStatValueType();
    }

    /**
     * Get the name that is stored in the DB
     *
     * @return
     */
    @Override
    public @NotNull String getStatType() {
        return "GENERIC " + stat.getStatType();
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return null;
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
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        while (true) {
            if (stat.containsStat(otherStat)) {
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
