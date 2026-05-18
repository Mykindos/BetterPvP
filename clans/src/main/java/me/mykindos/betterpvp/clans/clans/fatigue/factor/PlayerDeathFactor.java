package me.mykindos.betterpvp.clans.clans.fatigue.factor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigue;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathContext;
import me.mykindos.betterpvp.core.config.Config;

import java.util.UUID;

/**
 * Signals that the player died against another player.
 */
@Singleton
public class PlayerDeathFactor implements FatigueFactor {

    @Inject
    @Config(path = "clans.fatigue.factor.playerDeath.weight", defaultValue = "30.0")
    private double weight;

    @Override
    public String getName() {
        return "playerDeath";
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

        return 1.0;
    }
}
