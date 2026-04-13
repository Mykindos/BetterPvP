package me.mykindos.betterpvp.shops.shops.menus;

import lombok.Getter;
import me.mykindos.betterpvp.core.components.shops.IShopItem;

import java.util.Comparator;

@Getter
public enum SortMode {

    ORDER("Default Order", Comparator.comparingInt(IShopItem::getOrder)),
    PRICE_ASC("Price: Low to High", Comparator.comparingInt(IShopItem::getBuyPrice)),
    PRICE_DESC("Price: High to Low", Comparator.comparingInt(IShopItem::getBuyPrice).reversed());

    private final String displayName;
    private final Comparator<IShopItem> comparator;

    SortMode(String displayName, Comparator<IShopItem> comparator) {
        this.displayName = displayName;
        this.comparator = comparator;
    }

    public SortMode next() {
        SortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public SortMode previous() {
        SortMode[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }
}
