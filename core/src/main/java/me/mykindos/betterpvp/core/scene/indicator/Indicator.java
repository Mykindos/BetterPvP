package me.mykindos.betterpvp.core.scene.indicator;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * A marker hovering over something, for as long as it is worth showing.
 *
 * @see IndicatorService
 */
public interface Indicator {

    /**
     * Limits who can see this. Re-evaluated periodically, so a filter reading changing state (whether a captain has
     * requests waiting, say) keeps up on its own without anyone telling the indicator to refresh.
     * <p>
     * The default is everyone.
     */
    void visibleTo(@NotNull Predicate<Player> filter);

    /** Takes it down. Safe to call twice. */
    void remove();

    boolean isActive();
}
