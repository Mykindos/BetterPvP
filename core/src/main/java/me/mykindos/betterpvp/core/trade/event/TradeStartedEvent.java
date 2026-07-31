package me.mykindos.betterpvp.core.trade.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.trade.TradeSession;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when two players open a trade window together.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class TradeStartedEvent extends CustomEvent {

    private final TradeSession session;

    public TradeStartedEvent(@NotNull TradeSession session) {
        this.session = session;
    }
}
