package me.mykindos.betterpvp.core.item.component.impl.stat.type;

import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypeHandler;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.format.TextColor;

/**
 * Type handler for Double-based stats.
 */
public class DoubleStatTypeHandler implements StatTypeHandler<Double> {

    private static final DoubleStatTypeHandler INSTANCE = new DoubleStatTypeHandler();

    private DoubleStatTypeHandler() {}

    public static DoubleStatTypeHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Double getZero() {
        return 0.0;
    }

    @Override
    public Double getOne() {
        return 1.0;
    }

    @Override
    public Double add(Double a, Double b) {
        return a + b;
    }

    @Override
    public Double subtract(Double a, Double b) {
        return a - b;
    }

    @Override
    public Double multiply(Double value, double scalar) {
        return value * scalar;
    }

    @Override
    public Double fromDouble(double value) {
        return value;
    }

    @Override
    public String formatValue(Double value, boolean isPercentage) {
        if (isPercentage) {
            String text = UtilFormat.formatNumber(value * 100, 2, false);
            return (value > 0 ? "+" : "") + text + "%";
        } else {
            String text = UtilFormat.formatNumber(value, 2, false);
            return (value > 0 ? "+" : "") + text;
        }
    }

    @Override
    public TextColor getDefaultColor(Double value) {
        return value >= 0 ? ItemStat.GREEN : ItemStat.RED;
    }

    @Override
    public Double min(Double a, Double b) {
        return Math.min(a, b);
    }

    @Override
    public Double randomBetween(Double min, Double max) {
        if (min.equals(max)) {
            return min;
        }
        return min + (Math.random() * (max - min));
    }

    @Override
    public Double randomBetweenBiased(Double min, Double max, double bias) {
        if (min.equals(max)) {
            return min;
        }

        // Clamp bias to [0.0, 1.0]
        bias = Math.max(0.0, Math.min(1.0, bias));

        // Linear interpolation using the bias ratio
        // bias=0.0 returns min, bias=1.0 returns max
        return min + (bias * (max - min));
    }

    @Override
    public double toDouble(Double value) {
        return value;
    }
}
