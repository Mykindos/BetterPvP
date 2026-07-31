package me.mykindos.betterpvp.core.trade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.trade.currency.BalanceCurrency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every {@link TradeCurrency} a player can put on the table, in the order the trade window shows them.
 */
@Singleton
public class TradeCurrencyRegistry {

    private final Map<String, TradeCurrency> currencies = new LinkedHashMap<>();

    @Inject
    public TradeCurrencyRegistry(BalanceCurrency balance) {
        register(balance);
    }

    public void register(@NotNull TradeCurrency currency) {
        currencies.put(currency.getKey(), currency);
    }

    @Nullable
    public TradeCurrency get(@NotNull String key) {
        return currencies.get(key);
    }

    @NotNull
    public Collection<TradeCurrency> all() {
        return Collections.unmodifiableCollection(currencies.values());
    }
}
