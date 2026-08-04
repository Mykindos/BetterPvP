package me.mykindos.betterpvp.clans.world.discovery.sighting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.config.Config;

/**
 * The distances a sighting lives by, in one place because both the field and the marker read them.
 * <p>
 * They only make sense in one order: {@code arrival < rampStart < spawnMin < despawn}. A sighting placed inside the
 * arrival distance would land the crew on the tick it appeared, and one placed beyond the despawn distance would be
 * deleted on the same tick — either way the sea reads as empty for reasons nobody can see.
 * <p>
 * The builder states every distance by name, so a set of thresholds can be laid out without a server behind them.
 */
@Singleton
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(access = AccessLevel.PACKAGE)
public class SightingConfig {

    /** Expected sightings per second, so roughly one every half-minute of sailing. */
    @Inject
    @Config(path = "clans.discovery.sighting-rate", defaultValue = "0.03")
    private double sightingsPerSecond;

    /** Nearest a new island may appear. */
    @Inject
    @Config(path = "clans.discovery.sighting-spawn-min-distance", defaultValue = "600.0")
    private double spawnMinDistance;

    /** Furthest a new island may appear. */
    @Inject
    @Config(path = "clans.discovery.sighting-spawn-max-distance", defaultValue = "1200.0")
    private double spawnMaxDistance;

    /** How far an island may fall astern before it is taken off the sea. */
    @Inject
    @Config(path = "clans.discovery.sighting-despawn-distance", defaultValue = "1500.0")
    private double despawnDistance;

    /** True distance at which the crew has arrived. */
    @Inject
    @Config(path = "clans.discovery.sighting-arrival-distance", defaultValue = "100.0")
    private double arrivalDistance;

    /** True distance at which the marker starts growing. */
    @Inject
    @Config(path = "clans.discovery.sighting-ramp-start-distance", defaultValue = "200.0")
    private double rampStartDistance;

    /**
     * How far from the ship the marker is drawn, whatever the true distance. The marker is a bearing, not a position:
     * an island six hundred blocks out would otherwise be drawn where no client would ever be sent it.
     */
    @Inject
    @Config(path = "clans.discovery.sighting-pin-distance", defaultValue = "100.0")
    private double pinDistance;
}
