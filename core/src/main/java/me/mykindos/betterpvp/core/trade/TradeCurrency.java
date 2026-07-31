package me.mykindos.betterpvp.core.trade;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * A non-item value that can be put on the table in a trade.
 * <p>
 * Items move themselves - they are escrowed in the trade window and handed over as-is. Everything
 * else a player might offer is a balance held somewhere off the table, and this is the seam for it:
 * one implementation per kind of wealth, registered with {@link TradeCurrencyRegistry}. Adding war
 * points, or any other currency later, is a new implementation and a registration - the session, the
 * settlement, and the trade window already handle any number of these.
 */
public interface TradeCurrency {

    /**
     * @return Stable identifier, used for lookup and for persisting an amount against a currency.
     */
    @NotNull
    String getKey();

    @NotNull
    Component getDisplayName();

    /**
     * @return The material shown for this currency in the trade window.
     */
    @NotNull
    Material getIcon();

    /**
     * @return How much of this currency {@code gamer} currently holds.
     */
    int getBalance(@NotNull Gamer gamer);

    /**
     * Moves {@code amount} from one gamer to another.
     * <p>
     * Both sides of the move belong to the currency rather than to the settlement, because how a
     * balance is debited is the currency's business - a future currency may be capped, taxed, or
     * held per-clan rather than per-gamer, and settlement should not have to know.
     * <p>
     * Callers guarantee {@code from} has been checked against {@link #getBalance} in the same tick.
     */
    void transfer(@NotNull Gamer from, @NotNull Gamer to, int amount);
}
