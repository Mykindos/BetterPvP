package me.mykindos.betterpvp.core.client.stats.display.filter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.menu.button.filter.IFilterContext;
import me.mykindos.betterpvp.core.server.Realm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@EqualsAndHashCode
@Getter
public class RealmContext implements IFilterContext<RealmContext> {
    public static final RealmContext ALL = new RealmContext(null);

    @Nullable("When all")
    private final Realm realm;

    private final StatFilterType statFilterType;

    public RealmContext(@Nullable Realm realm) {
        this.statFilterType = realm != null ? StatFilterType.REALM : StatFilterType.SEASON;
        this.realm = realm;
    }

    /**
     * Get the string representation to show for this object
     *
     * @return the element to display for this object
     */
    @Override
    public String getDisplay() {
        if (statFilterType == StatFilterType.SEASON) return "All";
        Objects.requireNonNull(realm);

        return realm.getSeason().getName() + " " + realm.getServer().getName();
    }

    @Override
    public RealmContext getType() {
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
     */
    @Override
    public int compareTo(@NotNull IFilterContext<RealmContext> o) {
        final StatFilterType otherType = o.getType().getStatFilterType();
        if (statFilterType == StatFilterType.SEASON) {
            //Season is less than any other type
            return otherType == StatFilterType.SEASON ? 0 : -1;
        }
        //Realm is greater than Season
        if (otherType == StatFilterType.SEASON) return -1;
        //else compare by id
        return Integer.compare(Objects.requireNonNull(realm).getServer().getId(), Objects.requireNonNull(o.getType().getRealm()).getServer().getId());
    }
}
