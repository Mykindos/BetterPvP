package me.mykindos.betterpvp.shops;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShopsTranslationKeys {

    public static final String ERROR_INSUFFICIENT_FUNDS = "shops.error.insufficient_funds";
    public static final String ERROR_INVENTORY_FULL = "shops.error.inventory_full";
    public static final String ERROR_CANNOT_SELL = "shops.error.cannot_sell";

    public static final String MESSAGE_PURCHASE_SUCCESS = "shops.message.purchase_success";
    public static final String MESSAGE_SELL_SUCCESS = "shops.message.sell_success";

    public static final String BROADCAST_DYNAMIC_PRICES_UPDATED = "shops.broadcast.dynamic_prices_updated";
}
