package me.mykindos.betterpvp.core.item.component.impl.stat.type;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import net.kyori.adventure.text.format.TextColor;

@EqualsAndHashCode(callSuper = true)
public abstract class IntegerItemStat extends ItemStat<Integer> {

    protected IntegerItemStat(String keyName, String name, String shortName, String description, int value) {
        super(keyName, name, shortName, description, value);
    }

    protected IntegerItemStat(String keyName, String name, String description, int value) {
        super(keyName, name, name.toLowerCase(), description, value);
    }

    @Override
    protected TextColor getValueColor() {
        return getValue() >= 0 ? ItemStat.GREEN : ItemStat.RED;
    }

    @Override
    public String stringValue() {
        final String text = String.valueOf(getValue());
        if (getValue() > 0) {
            return "+" + text;
        } else if (getValue() < 0) {
            return text;
        } else {
            return "0";
        }
    }

    @Override
    public abstract IntegerItemStat withValue(Integer newValue);

    @Override
    public ItemStat<Integer> merge(ItemStat<?> other) {
        if (other == null || getClass() != other.getClass()) {
            throw new UnsupportedOperationException("Cannot merge different stat types: " + getClass() + " and " + (other == null ? "null" : other.getClass()));
        }
        IntegerItemStat otherStat = (IntegerItemStat) other;
        return withValue(this.getValue() + otherStat.getValue());
    }
}
