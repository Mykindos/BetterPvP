package me.mykindos.betterpvp.clans.world.discovery.sighting;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.discovery.Expedition;
import me.mykindos.betterpvp.clans.world.discovery.ExpeditionService;
import me.mykindos.betterpvp.clans.world.discovery.Ocean;
import me.mykindos.betterpvp.clans.world.island.IslandOfferProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The islands on every running expedition's horizon: rolling for them, swinging them around the bearing as the ship
 * turns, and putting the crew ashore on the one they steer for.
 * <p>
 * A field is attached on the first tick that asks for one and released when the expedition ends, exactly as the hazards
 * are — markers left behind would sit in a world about to be recycled.
 */
@Singleton
public class SightingService {

    private final SightingConfig config;
    private final IslandOfferProvider offers;
    private final Provider<ExpeditionService> expeditions;

    private final Map<UUID, SightingField> fields = new ConcurrentHashMap<>();

    @Inject
    public SightingService(@NotNull SightingConfig config, @NotNull IslandOfferProvider offers,
                           @NotNull Provider<ExpeditionService> expeditions) {
        this.config = config;
        this.offers = offers;
        this.expeditions = expeditions;
    }

    /**
     * Rolls for a new island, redraws everything already sighted, and lands the crew on anything they have reached.
     *
     * @param dt seconds elapsed, the same step the ship was advanced by
     */
    public void tick(@NotNull Expedition expedition, double dt, long now) {
        final SightingField field = field(expedition);
        final Ocean ocean = expedition.getOcean();
        final double heading = expedition.getDynamics().getHeading();

        spawn(field, ocean, heading, dt);

        // Every quarter-second: the number changes by a few metres a tick and every sailor pays for each rewrite.
        field.sweep(point -> ocean.localOf(point, heading), expedition::render, config, field.dueForLabels(now, 250L))
                .ifPresent(sighting -> expeditions.get().makeLandfall(expedition, sighting.getOffer()));
    }

    /** Clears an expedition's horizon, taking every marker's entities with it. */
    public void release(@NotNull Expedition expedition) {
        final SightingField field = fields.remove(expedition.getCrew().getCaptain());
        if (field != null) {
            field.clear();
        }
    }

    /**
     * Rolls for one island, anywhere around the ship.
     * <p>
     * Uniform in bearing and independent of what is already out there: an island the crew has to come about for is as
     * good a reason to steer as one dead ahead, and nothing about the sea says two of them cannot share a horizon.
     */
    private void spawn(@NotNull SightingField field, @NotNull Ocean ocean, double heading, double dt) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= config.getSightingsPerSecond() * dt) {
            return;
        }

        final double bearing = random.nextDouble(-180.0, 180.0);
        final double distance = random.nextDouble(config.getSpawnMinDistance(), config.getSpawnMaxDistance());
        offers.draw().ifPresent(offer -> field.add(new Sighting(ocean.pointAt(heading, bearing, distance), offer)));
    }

    private @NotNull SightingField field(@NotNull Expedition expedition) {
        return fields.computeIfAbsent(expedition.getCrew().getCaptain(), captain -> new SightingField());
    }
}
