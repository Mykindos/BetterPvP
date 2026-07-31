package me.mykindos.betterpvp.core.trade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A standing ask from one player to trade with another. The target may accept it or leave it to expire;
 * there is deliberately no counter-offer at this stage - negotiating happens in the trade window.
 */
@Getter
@RequiredArgsConstructor
public class TradeRequest {

    private final UUID requester;
    private final UUID target;
    private final long createdAt = System.currentTimeMillis();

    public boolean hasExpired(long lifetimeMillis) {
        return System.currentTimeMillis() - createdAt > lifetimeMillis;
    }

    public boolean isBetween(@NotNull UUID requester, @NotNull UUID target) {
        return this.requester.equals(requester) && this.target.equals(target);
    }
}
