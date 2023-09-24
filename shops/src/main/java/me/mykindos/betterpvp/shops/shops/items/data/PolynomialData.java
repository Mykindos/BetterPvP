package me.mykindos.betterpvp.shops.shops.items.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PolynomialData {

    private final int minBuyPrice;
    private final int baseBuyPrice;
    private final int maxBuyPrice;
    private final int minSellPrice;
    private final int baseSellPrice;
    private final int maxSellPrice;

    private final int maxStock;
    private final int baseStock;
    private int currentStock;

}
