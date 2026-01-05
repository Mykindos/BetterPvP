package me.mykindos.betterpvp.core.item.component.impl.currency;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.item.ItemInstance;

@UtilityClass
public class CurrencyUtils {

    public static final String CURRENCY = "Gold";

    public static long getValue(ItemInstance itemInstance) {
        CurrencyComponent currencyComponent = itemInstance.getComponent(CurrencyComponent.class)
                .orElseThrow(() -> new IllegalStateException("ItemInstance does not have a CurrencyComponent"));
        return currencyComponent.getValue() * itemInstance.getItemStack().getAmount();
    }

    public static boolean canSubtract(ItemInstance itemInstance, long amount) {
        CurrencyComponent currencyComponent = itemInstance.getComponent(CurrencyComponent.class)
                .orElseThrow(() -> new IllegalStateException("ItemInstance does not have a CurrencyComponent"));
        long unitValue = currencyComponent.getValue();
        long totalValue = unitValue * itemInstance.getItemStack().getAmount();
        if (amount > totalValue) {
            return false;
        }

        long remainingValue = totalValue - amount;
        return remainingValue % unitValue == 0;
    }

    public static ItemInstance subtract(ItemInstance itemInstance, long amount) {
        CurrencyComponent currencyComponent = itemInstance.getComponent(CurrencyComponent.class)
                .orElseThrow(() -> new IllegalStateException("ItemInstance does not have a CurrencyComponent"));
        long unitValue = currencyComponent.getValue();
        long totalValue = unitValue * itemInstance.getItemStack().getAmount();

        if (amount > totalValue) {
            throw new IllegalArgumentException("Cannot subtract more currency than the item instance is worth");
        }

        long remainingValue = totalValue - amount;

        if (remainingValue % unitValue != 0) {
            throw new IllegalStateException(
                    "Resulting amount is not a whole number: remainingValue=" + remainingValue +
                            ", unitValue=" + unitValue
            );
        }

        int newAmount = (int) (remainingValue / unitValue);
        itemInstance.getItemStack().setAmount(newAmount);
        return itemInstance;
    }

}
