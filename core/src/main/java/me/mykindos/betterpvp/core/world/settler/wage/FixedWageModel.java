package me.mykindos.betterpvp.core.world.settler.wage;

import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** One rate per profession and rarity, whatever the settler is doing. */
public class FixedWageModel implements WageModel {

    private final Map<String, Map<SettlerRarity, Double>> rates;

    public FixedWageModel(@NotNull Map<String, Map<SettlerRarity, Double>> rates) {
        this.rates = Map.copyOf(rates);
    }

    @Override
    public double perHour(@NotNull Settler settler, boolean working) {
        return settler.getProfession() == null ? 0
                : rates.getOrDefault(settler.getProfession(), Map.of()).getOrDefault(settler.getRarity(), 0.0);
    }
}
