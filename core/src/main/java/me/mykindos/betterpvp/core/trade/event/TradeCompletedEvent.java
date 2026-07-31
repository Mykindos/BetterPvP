package me.mykindos.betterpvp.core.trade.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.trade.TradeSession;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a trade has settled and both sides have received what they were owed.
 * <p>
 * Fired after the swap, not before, so a listener can never leave a trade half-applied. Anything that
 * needs to veto a trade belongs in {@link me.mykindos.betterpvp.core.trade.TradeSettlement}'s checks.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class TradeCompletedEvent extends CustomEvent {

    private final TradeSession session;

    public TradeCompletedEvent(@NotNull TradeSession session) {
        this.session = session;
    }
}
