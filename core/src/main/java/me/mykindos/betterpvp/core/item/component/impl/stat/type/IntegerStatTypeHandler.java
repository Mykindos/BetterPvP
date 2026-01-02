package me.mykindos.betterpvp.core.item.component.impl.stat.type;

import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypeHandler;
import net.kyori.adventure.text.format.TextColor;

/**
 * Type handler for Integer-based stats.
 */
public class IntegerStatTypeHandler implements StatTypeHandler<Integer> {

    private static final IntegerStatTypeHandler INSTANCE = new IntegerStatTypeHandler();

    private IntegerStatTypeHandler() {}

    public static IntegerStatTypeHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Integer getZero() {
        return 0;
    }

    @Override
    public Integer getOne() {
        return 1;
    }

    @Override
    public Integer add(Integer a, Integer b) {
        return a + b;
    }

    @Override
    public Integer subtract(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public Integer multiply(Integer value, double scalar) {
        return (int) (value * scalar);
    }

    @Override
    public Integer fromDouble(double value) {
        return (int) value;
    }

    @Override
    public String formatValue(Integer value, boolean isPercentage) {
        if (isPercentage) {
            String text = String.format("%d", value * 100);
            return (value > 0 ? "+" : "") + text + "%";
        } else {
            return (value > 0 ? "+" : "") + value;
        }
    }

    @Override
    public TextColor getDefaultColor(Integer value) {
        return value >= 0 ? ItemStat.GREEN : ItemStat.RED;
    }

    @Override
    public Integer min(Integer a, Integer b) {
        return Math.min(a, b);
    }

    @Override
    public Integer randomBetween(Integer min, Integer max) {
        if (min.equals(max)) {
            return min;
        }
        return min + (int) (Math.random() * (max - min + 1));
    }

    @Override
    public Integer randomBetweenBiased(Integer min, Integer max, double bias) {
        if (min.equals(max)) {
            return min;
        }

        // Clamp bias to [0.0, 1.0]
        bias = Math.max(0.0, Math.min(1.0, bias));

        // Linear interpolation with proper rounding for integers
        // bias=0.0 returns min, bias=1.0 returns max
        double range = max - min;
        return min + (int) Math.round(bias * range);
    }
}
