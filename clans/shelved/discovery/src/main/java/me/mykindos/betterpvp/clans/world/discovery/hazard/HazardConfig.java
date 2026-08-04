package me.mykindos.betterpvp.clans.world.discovery.hazard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.Config;

/**
 * The knobs hazards are tuned with, in one place because they are read from the archetypes rather than from a service.
 */
@Singleton
@Getter
public class HazardConfig {

    /**
     * How far a hazard may drift before it is taken off the sea.
     * <p>
     * Must stay comfortably above {@code HazardSpawnPolicy}'s spawn ranges. Hazards ahead are placed at a hundred
     * blocks, so a radius of a hundred would delete every one of them on the tick it was created and the sea would
     * look empty for reasons nobody could see.
     */
    @Inject
    @Config(path = "clans.discovery.hazard-despawn-radius", defaultValue = "120.0")
    private double despawnRadius;

    /** How long a whirlpool keeps hold of the wheel. */
    @Inject
    @Config(path = "clans.discovery.whirlpool-hold-millis", defaultValue = "2000")
    private long whirlpoolHoldMillis;

    /** Blindness on being stranded, covering the change of world on the way back. */
    @Inject
    @Config(path = "clans.discovery.stranded-blindness-millis", defaultValue = "4000")
    private long strandedBlindnessMillis;

    /** Nausea on being stranded, running on past the blindness so the return is still unsteady. */
    @Inject
    @Config(path = "clans.discovery.stranded-nausea-millis", defaultValue = "8000")
    private long strandedNauseaMillis;
}
