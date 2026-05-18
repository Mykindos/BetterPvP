package me.mykindos.betterpvp.clans.clans.fatigue.factor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigue;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathContext;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathRecord;
import me.mykindos.betterpvp.core.config.Config;

import java.util.UUID;

/**
 * Signals that the player keeps feeding the <i>same</i> opponent. Returns the
 * fraction of recent deaths attributable to the current killer, so dying to one
 * person over and over saturates toward 1.0 while a varied death log stays low.
 */
@Singleton
public class RepeatKillerFactor implements FatigueFactor {

    @Inject
    @Config(path = "clans.fatigue.factor.repeatKiller.saturationDeaths", defaultValue = "4")
    private int saturationDeaths;

    @Inject
    @Config(path = "clans.fatigue.factor.repeatKiller.weight", defaultValue = "15.0")
    private double weight;

    @Override
    public String getName() {
        return "repeatKiller";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public double evaluate(BattleFatigue state, DeathContext context) {
        final UUID killer = context.killer();
        if (killer == null) {
            return 0.0;
        }

        long sameKiller = 0;
        for (DeathRecord record : state.getDeathHistory()) {
            if (killer.equals(record.killer())) {
                sameKiller++;
            }
        }

        // Saturates at 1.0 once the player has fed this killer `saturationDeaths` times.
        return Math.min(1.0, (double) sameKiller / Math.max(1, saturationDeaths));
    }
}
