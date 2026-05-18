package me.mykindos.betterpvp.clans.clans.fatigue.factor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigue;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathContext;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathRecord;
import me.mykindos.betterpvp.core.config.Config;

/**
 * Signals rapid re-queueing into death. Counts deaths in the short window
 * leading up to this one; the magnitude rises super-linearly so the first death
 * barely registers but a burst of them spikes hard.
 */
@Singleton
public class DeathFrequencyFactor implements FatigueFactor {

    @Inject
    @Config(path = "clans.fatigue.factor.frequency.windowSeconds", defaultValue = "120")
    private int windowSeconds;

    @Inject
    @Config(path = "clans.fatigue.factor.frequency.saturationDeaths", defaultValue = "4")
    private int saturationDeaths;

    @Inject
    @Config(path = "clans.fatigue.factor.frequency.weight", defaultValue = "15.0")
    private double weight;

    @Override
    public String getName() {
        return "frequency";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public double evaluate(BattleFatigue state, DeathContext context) {
        final long cutoff = context.timestamp() - (windowSeconds * 1000L);

        long recent = 0;
        for (DeathRecord record : state.getDeathHistory()) {
            if (record.timestamp() >= cutoff) {
                recent++;
            }
        }

        final double ratio = Math.min(1.0, (double) recent / Math.max(1, saturationDeaths));
        // Super-linear: a single recent death is cheap, a cluster is punishing.
        return ratio * ratio;
    }
}
