package me.mykindos.betterpvp.core.world.settler.wage;

import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** A lower rate while a settler waits for work and a higher one while it works, per profession and rarity. */
public class IdleWorkingWageModel implements WageModel {

    private final FixedWageModel idle;
    private final FixedWageModel working;

    public IdleWorkingWageModel(@NotNull Map<String, Map<SettlerRarity, Double>> idle,
                                @NotNull Map<String, Map<SettlerRarity, Double>> working) {
        this.idle = new FixedWageModel(idle);
        this.working = new FixedWageModel(working);
    }

    @Override
    public double perHour(@NotNull Settler settler, boolean working) {
        return (working ? this.working : idle).perHour(settler, working);
    }
}
