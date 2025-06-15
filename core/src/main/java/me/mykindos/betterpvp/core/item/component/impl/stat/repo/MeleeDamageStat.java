package me.mykindos.betterpvp.core.item.component.impl.stat.repo;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.DoubleItemStat;

@EqualsAndHashCode(callSuper = true)
public class MeleeDamageStat extends DoubleItemStat {

    public MeleeDamageStat(double value) {
        super("melee-damage", "Damage", "Increases the melee damage dealt by the item.", value);
    }

    @Override
    protected boolean isValidValue(Double value) {
        return super.isValidValue(value) && value >= 0;
    }

    @Override
    public MeleeDamageStat copy() {
        return new MeleeDamageStat(getValue());
    }

    @Override
    public MeleeDamageStat withValue(Double newValue) {
        return new MeleeDamageStat(newValue);
    }
}
