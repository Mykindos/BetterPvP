package me.mykindos.betterpvp.core.item.component.impl.stat.type;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.format.TextColor;

@EqualsAndHashCode(callSuper = true)
public abstract class DoubleItemStat extends ItemStat<Double> {

    protected DoubleItemStat(String keyName, String name, String shortName, String description, double value) {
        super(keyName, name, shortName, description, value);
    }

    protected DoubleItemStat(String keyName, String name, String description, double value) {
        super(keyName, name, name, description, value);
    }

    @Override
    public String stringValue() {
        final String text = UtilFormat.formatNumber(getValue(), 1, false);
        if (getValue() > 0) {
            return "+" + text;
        } else if (getValue() < 0) {
            return text;
        } else {
            return "0";
        }
    }

    @Override
    protected TextColor getValueColor() {
        return getValue() >= 0 ? ItemStat.GREEN : ItemStat.RED;
    }

    @Override
    public abstract DoubleItemStat withValue(Double newValue);

    @Override
    public ItemStat<Double> merge(ItemStat<?> other) {
        if (other == null || getClass() != other.getClass()) {
            throw new UnsupportedOperationException("Cannot merge different stat types: " + getClass() + " and " + (other == null ? "null" : other.getClass()));
        }
        DoubleItemStat otherStat = (DoubleItemStat) other;
        return withValue(this.getValue() + otherStat.getValue());
    }
}
