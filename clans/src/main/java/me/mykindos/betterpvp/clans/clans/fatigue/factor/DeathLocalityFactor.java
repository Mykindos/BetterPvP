package me.mykindos.betterpvp.clans.clans.fatigue.factor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigue;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathContext;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathRecord;
import me.mykindos.betterpvp.core.config.Config;
import org.bukkit.Location;

/**
 * Signals that the player keeps dying in the <i>same place</i> — i.e. sprinting
 * straight back into the meat grinder. Counts how many recent deaths occurred
 * within {@code radius} blocks of this one and saturates toward 1.0.
 */
@Singleton
public class DeathLocalityFactor implements FatigueFactor {

    @Inject
    @Config(path = "clans.fatigue.factor.locality.radius", defaultValue = "80.0")
    private double radius;

    @Inject
    @Config(path = "clans.fatigue.factor.locality.saturationDeaths", defaultValue = "3")
    private int saturationDeaths;

    @Inject
    @Config(path = "clans.fatigue.factor.locality.weight", defaultValue = "12.0")
    private double weight;

    @Override
    public String getName() {
        return "locality";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public double evaluate(BattleFatigue state, DeathContext context) {
        final Location here = context.location();
        final double radiusSq = radius * radius;

        long nearby = 0;
        for (DeathRecord record : state.getDeathHistory()) {
            final Location past = record.location();
            if (past.getWorld() != null && past.getWorld().equals(here.getWorld())
                    && past.distanceSquared(here) <= radiusSq) {
                nearby++;
            }
        }

        return Math.min(1.0, (double) nearby / Math.max(1, saturationDeaths));
    }
}
