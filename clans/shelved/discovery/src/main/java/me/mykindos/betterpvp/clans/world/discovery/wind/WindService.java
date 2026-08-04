package me.mykindos.betterpvp.clans.world.discovery.wind;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.discovery.Expedition;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The weather on every running expedition: picking a new wind now and then, easing the ship onto it, and telling the
 * crew when the sailing has actually changed.
 * <p>
 * Only a change of {@link WindBand} is worth saying out loud. The factor is nudged every tick, and reporting each nudge
 * would fill the chat with weather nobody can feel.
 */
@Singleton
public class WindService {

    /** How long one wind lasts before another is rolled. */
    @Inject
    @Config(path = "clans.discovery.wind-period-millis", defaultValue = "45000")
    private long periodMillis;

    /** The slowest the weather will make a ship, as a multiple of its base speed. */
    @Inject
    @Config(path = "clans.discovery.wind-min-factor", defaultValue = "0.55")
    private double minFactor;

    @Inject
    @Config(path = "clans.discovery.wind-max-factor", defaultValue = "1.50")
    private double maxFactor;

    /** Share of the gap to the new wind closed each second, so the ship takes a few seconds to answer it. */
    @Inject
    @Config(path = "clans.discovery.wind-ease-rate", defaultValue = "0.25")
    private double easeRate;

    private final Map<UUID, Wind> winds = new ConcurrentHashMap<>();

    /**
     * Rolls for a new wind when one is due, eases the ship onto it, and reports a band the crew has just sailed into.
     *
     * @param crew the sailors still aboard, the only audience for any of it
     * @param dt   seconds elapsed, the same step the ship is advanced by
     */
    public void tick(@NotNull Expedition expedition, @NotNull List<Player> crew, double dt, long now) {
        final Wind wind = winds.computeIfAbsent(expedition.getCrew().getCaptain(), captain -> new Wind());
        if (wind.due(now)) {
            wind.shift(ThreadLocalRandom.current().nextDouble(minFactor, maxFactor), now, periodMillis);
        }

        wind.advance(dt, easeRate).ifPresent(band -> announce(crew, band));
        expedition.getDynamics().setWindFactor(wind.getFactor());
    }

    /** Forgets an expedition's weather, so the next crew under this captain starts in a fair wind. */
    public void release(@NotNull Expedition expedition) {
        winds.remove(expedition.getCrew().getCaptain());
    }

    /** One phrasing, drawn per band change rather than per crew member, so the whole ship hears the same thing. */
    private void announce(@NotNull List<Player> crew, @NotNull WindBand band) {
        final String key = band.messageKey(ThreadLocalRandom.current().nextInt(band.variants()));
        for (Player sailor : crew) {
            UtilMessage.message(sailor, "clans.prefix.ship", key);
        }
    }
}
