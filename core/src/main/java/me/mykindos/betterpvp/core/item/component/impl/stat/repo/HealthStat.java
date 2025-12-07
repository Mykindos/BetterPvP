package me.mykindos.betterpvp.core.item.component.impl.stat.repo;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.IntegerItemStat;

@EqualsAndHashCode(callSuper = true)
public class HealthStat extends IntegerItemStat {

    public HealthStat(int base) {
        super("health", "Health", "Increases the health of the wearer.", base);
    }

    @Override
    public ItemStat<Integer> copy() {
        return new HealthStat(getValue());
    }

    @Override
    public IntegerItemStat withValue(Integer newValue) {
        return new HealthStat(newValue);
    }
}
