package me.mykindos.betterpvp.core.trade.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.trade.TradeCancelReason;
import me.mykindos.betterpvp.core.trade.TradeSession;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a trade ended without settling and both escrows have been returned.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class TradeCancelledEvent extends CustomEvent {

    private final TradeSession session;
    private final TradeCancelReason reason;

    public TradeCancelledEvent(@NotNull TradeSession session, @NotNull TradeCancelReason reason) {
        this.session = session;
        this.reason = reason;
    }
}
