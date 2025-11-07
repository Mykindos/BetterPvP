package me.mykindos.betterpvp.core.client.stats.display.filter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClansStat;
import me.mykindos.betterpvp.core.menu.button.filter.IFilterContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode
@Getter
public class ClanContext implements IFilterContext<ClanContext> {
    public static final ClanContext ALL = new ClanContext(ContextType.ALL);
    public static final ClanContext NO_CLAN = new ClanContext(ContextType.NO_CLAN);

    private final String clanName;
    @Nullable
    private final UUID clanId;
    private final ContextType contextType;

    public static ClanContext from(ClansStat clansStat) {
        if (clansStat.getClanId() == null || clansStat.getClanName().equals(ClansStat.NO_CLAN_NAME)) return NO_CLAN;
        return new ClanContext(clansStat.getClanName(), clansStat.getClanId());
    }

    public ClanContext(String clanName, @Nullable UUID clanId) {
        this.clanName = clanName;
        this.clanId = clanId;
        this.contextType = ContextType.CLAN;
    }

    private ClanContext(ContextType contextType) {
        this.clanName = contextType == ContextType.NO_CLAN ? ClansStat.NO_CLAN_NAME : "";
        this.clanId = null;
        this.contextType = contextType;
    }

    /**
     * Get the string representation to show for this object
     *
     * @return the element to display for this object
     */
    @Override
    public String getDisplay() {
        if (contextType == ContextType.ALL) return "All";
        if (contextType == ContextType.NO_CLAN) return "No Clan";
        final StringBuilder stringBuilder = new StringBuilder(clanName);
        if (clanId != null) {
            stringBuilder.append(" (")
                    .append(clanId.toString(), 0, 6)
                    .append(")");
        }
        return stringBuilder.toString();
    }

    @Override
    public ClanContext getType() {
        return this;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure {@link Integer#signum
     * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * x.compareTo(y)} must throw an exception if and only if {@code
     * y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code
     * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
     * == signum(y.compareTo(z))}, for all {@code z}.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     * @apiNote It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>
     *     Sorted by: ALL -> NO_CLAN -> CLAN -> CLAN etc.
     * </p>
     */
    @Override
    public int compareTo(@NotNull IFilterContext<ClanContext> o) {
        final ContextType otherContext = o.getType().getContextType();
        if ((contextType == ContextType.ALL || contextType == ContextType.NO_CLAN) && contextType.equals(otherContext)) return 0;
        //All is less than No Clan or Clan
        if (contextType == ContextType.ALL && (otherContext == ContextType.NO_CLAN || otherContext == ContextType.CLAN)) return -1;
        //No Clan is greater than All
        if (contextType == ContextType.NO_CLAN && otherContext == ContextType.ALL) return 1;
        //No Clan is less than Clan
        if (contextType == ContextType.NO_CLAN && otherContext == ContextType.CLAN) return -1;
        int nameCompare = clanName.compareTo(o.getType().getClanName());
        //if names are not the same, return that
        if (nameCompare != 0) return nameCompare;
        //else, by id
        return Objects.requireNonNull(clanId).compareTo(Objects.requireNonNull(o.getType().getClanId()));


    }

    public enum ContextType {
        /**
         * All clans/no clans
         */
        ALL,
        /**
         * No clan
         */
        NO_CLAN,
        /**
         * Specific Clan
         */
        CLAN
    }
}
