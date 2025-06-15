package me.mykindos.betterpvp.core.item.component.impl.stat.repo;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.PercentageItemStat;

@EqualsAndHashCode(callSuper = true)
public class MeleeAttackSpeedStat extends PercentageItemStat {
    public MeleeAttackSpeedStat(double base) {
        super("melee-attack-speed", "Melee Attack Speed", "Increases the melee attack speed of the holder.", base);
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
