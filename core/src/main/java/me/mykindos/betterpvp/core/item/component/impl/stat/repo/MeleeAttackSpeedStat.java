package me.mykindos.betterpvp.core.item.component.impl.stat.repo;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.PercentageItemStat;
import me.mykindos.betterpvp.core.utilities.UtilFormat;

@EqualsAndHashCode(callSuper = true)
public class MeleeAttackSpeedStat extends PercentageItemStat {
    public MeleeAttackSpeedStat(double base) {
        super("melee-attack-speed",
                "Melee Attack Speed",
                "attacks/second",
                "Increases the melee attack speed of the holder.",
                base);
    }


    @Override
    public String stringValue() {
        final double hitsPerSecond = 1000L / (DamageCause.DEFAULT_DELAY / (1 + getValue()));
        return UtilFormat.formatNumber(hitsPerSecond, 2, false);
    }

    @Override
    protected boolean isValidValue(Double value) {
        return value != null;
    }

    @Override
    public MeleeAttackSpeedStat copy() {
        return new MeleeAttackSpeedStat(getValue());
    }

    @Override
    public MeleeAttackSpeedStat withValue(Double newValue) {
        return new MeleeAttackSpeedStat(newValue);
    }
}
