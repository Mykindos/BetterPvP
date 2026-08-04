package me.mykindos.betterpvp.clans.world.discovery.hazard;

import me.mykindos.betterpvp.clans.world.discovery.Expedition;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A turning column of water that takes the wheel out of the crew's hands.
 * <p>
 * Nothing is spawned for it. It is spray and noise on the surface, which is also why it has no teardown: drifting out
 * of range simply stops the particles arriving.
 */
public class Whirlpool extends Hazard {

    /** Off the surface by just enough that the spray is not fighting the water for the same pixels. */
    private static final double SURFACE_EPSILON = 0.1;

    /** How the archetype is registered, and everything one whirlpool needs to punish a captain. */
    public static class Archetype implements HazardArchetype {

        private final HazardConfig config;
        private final HazardService hazards;

        public Archetype(@NotNull HazardConfig config, @NotNull HazardService hazards) {
            this.config = config;
            this.hazards = hazards;
        }

        @Override
        public @NotNull String key() {
            return "whirlpool";
        }

        @Override
        public double radius() {
            return 5.0;
        }

        @Override
        public @NotNull Hazard create(@NotNull OceanPoint at) {
            return new Whirlpool(this, at, config, hazards);
        }
    }

    private final HazardConfig config;
    private final HazardService hazards;

    /** Ticks until the next churn, so the sound is irregular rather than a metronome under the crew's feet. */
    private int churnIn;

    private Whirlpool(@NotNull Archetype archetype, @NotNull OceanPoint at, @NotNull HazardConfig config,
                      @NotNull HazardService hazards) {
        super(archetype, at);
        this.config = config;
        this.hazards = hazards;
    }

    @Override
    public void render(@NotNull Location at) {
        final World world = at.getWorld();
        if (world == null) {
            return;
        }

        // The limbo world's only inhabitants are the crew, so this is the audience and nothing needs broadcasting.
        final List<Player> crew = world.getPlayers();
        if (crew.isEmpty()) {
            return;
        }

        final Location surface = at.clone().add(0, SURFACE_EPSILON, 0);
        Particle.SPLASH.builder()
                .location(surface)
                .count(160)
                .offset(2.2, 3.0, 2.2)
                .extra(0)
                .receivers(crew)
                .spawn();

        if (--churnIn <= 0) {
            churnIn = UtilMath.randomInt(8, 20);
            final SoundEffect churn = new SoundEffect(Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
                    (float) UtilMath.randDouble(0.4, 0.7), 3f);
            crew.forEach(sailor -> churn.play(sailor, surface));
        }
    }

    /**
     * Throws the ship into a hard turn to whichever side it feels like.
     * <p>
     * The seizure is on the net input rather than on the rudder, so the wheel is wound over against its own resistance
     * and the ship heels into the turn instead of jumping to hard-over on one tick.
     */
    @Override
    public void onCollide(@NotNull Expedition expedition) {
        final int side = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        hazards.override(expedition)
                .seize(side, System.currentTimeMillis(), config.getWhirlpoolHoldMillis());

        final World limbo = Bukkit.getWorld(expedition.getLimbo().getWorldName());
        if (limbo == null) {
            return;
        }

        for (Player sailor : limbo.getPlayers()) {
            UtilMessage.message(sailor, "clans.prefix.ship", "clans.discovery.whirlpool");
            new SoundEffect(Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.4f).play(sailor);
            new SoundEffect(Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 0.7f, 1.2f).play(sailor);
        }
    }

    @Override
    public void remove() {
        // Nothing was ever put in the world for it.
    }
}
