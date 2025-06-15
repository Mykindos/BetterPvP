package me.mykindos.betterpvp.core.item.component.impl.stat.type;

import lombok.EqualsAndHashCode;

public @EqualsAndHashCode(callSuper = true)
abstract class PercentageItemStat extends DoubleItemStat {

    protected PercentageItemStat(String keyName, String name, String shortName, String description, double value) {
        super(keyName, name, shortName, description, value);
    }

    protected PercentageItemStat(String keyName, String name, String description, double value) {
        super(keyName, name, description, value);
    }

    @Override
    protected boolean isValidValue(Double value) {
        return super.isValidValue(value) && value > 0;
    }

    @Override
    public String stringValue() {
        final String text = String.format("%d", (int) (getValue() * 100));
        if (getValue() > 0) {
            return "+" + text + "%";
        } else if (getValue() < 0) {
            return text + "%";
        } else {
            return "0%";
        }
    }
}
