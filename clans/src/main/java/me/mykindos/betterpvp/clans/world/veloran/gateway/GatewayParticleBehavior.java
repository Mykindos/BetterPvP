package me.mykindos.betterpvp.clans.world.veloran.gateway;

import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ambiance for The Sundered Gate: comets of teal portal-light are drawn in from the surrounding void and stream toward
 * the heart of the gate, where they collapse with a resonant chime — selling the "everything is being pulled into the
 * tear" look.
 * <p>
 * Each comet is a {@link Streamer}: it is born ~50 blocks in front of the portal (along the gate's facing), at a random
 * height, and travels in a straight line into a random point near the portal's centre while spinning around its own
 * travel axis. Several stream at once and each lives across many ticks, so the behaviour keeps a list of in-flight
 * comets and advances them every tick rather than drawing a one-shot puff.
 * <p>
 * All work is skipped when no player is close enough to see it, so an idle portal costs almost nothing.
 */
public class GatewayParticleBehavior implements SceneBehavior {

    /** Render budget guard: don't bother unless a player is within this radius of the portal centre. */
    private static final int VIEW_RANGE = 120;

    private final CuboidRegion portalRegion;
    private final Vector facing;
    private final List<Streamer> active = new ArrayList<>();

    /** Ticks until the next spawn attempt; randomised so comets don't pulse in lockstep. */
    private int spawnCooldown = 0;

    public GatewayParticleBehavior(CuboidRegion portalRegion, Vector facing) {
        this.portalRegion = portalRegion;
        // Flatten any pitch so "in front" reads as horizontal distance; fall back to north if the facing is degenerate.
        final Vector flat = new Vector(facing.getX(), 0, facing.getZ());
        this.facing = flat.lengthSquared() < 1.0e-6 ? new Vector(0, 0, -1) : flat.normalize();
    }

    @Override
    public void tick() {
        final World world = portalRegion.getWorld();
        if (world == null) {
            return;
        }

        final Location min = portalRegion.getMin();
        final Location max = portalRegion.getMax();
        final Vector center = new Vector(
                (min.getX() + max.getX()) / 2.0,
                (min.getY() + max.getY()) / 2.0,
                (min.getZ() + max.getZ()) / 2.0);

        final Location centerLoc = center.toLocation(world);
        final boolean anyoneWatching = world.getPlayers().stream()
                .anyMatch(player -> player.getLocation().distanceSquared(centerLoc) <= VIEW_RANGE * VIEW_RANGE);
        if (!anyoneWatching && active.isEmpty()) {
            return;
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        // Spawn new comets while anyone is around and we're under the cap of 6 in flight at once.
        if (anyoneWatching && active.size() < 6 && --spawnCooldown <= 0) {
            active.add(spawn(center, min, max, random));
            spawnCooldown = random.nextInt(8, 22);
        }

        // Advance every in-flight comet; retire those that have reached the portal.
        active.removeIf(comet -> comet.advance(world));
    }

    /**
     * Builds a comet ~50 (±10) blocks in front of the gate at a random height, aimed at a random point near the
     * portal's centre, with a randomised spin radius and rate. Plays the birth chime where it appears.
     */
    private Streamer spawn(Vector center, Location min, Location max, ThreadLocalRandom random) {
        // Target: a random point loosely clustered around the portal heart, never the exact centre.
        final Vector target = center.clone().add(new Vector(
                (random.nextDouble() - 0.5) * (max.getX() - min.getX()) * 0.5,
                (random.nextDouble() - 0.5) * (max.getY() - min.getY()) * 0.5,
                (random.nextDouble() - 0.5) * (max.getZ() - min.getZ()) * 0.5));

        // Origin: out along the gate's facing, lifted/dropped by a random height, with a little lateral scatter.
        final double distance = 70 + random.nextDouble(-10, 30);
        final Vector lateral = new Vector(-facing.getZ(), 0, facing.getX()); // facing rotated 90° on the horizontal
        final Vector origin = center.clone()
                .add(facing.clone().multiply(distance))
                .add(lateral.multiply((random.nextDouble() - 0.5) * 100))
                .add(new Vector(0, random.nextDouble(-20, 20), 0));

        final World world = min.getWorld();
        new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_CHIME, (float) random.nextDouble(1.2, 1.7), 0.7f)
                .play(origin.toLocation(world));

        return new Streamer(origin, target, random);
    }

    @Override
    public void stop() {
        active.clear();
    }

    /**
     * A single comet in flight. Walks from {@code origin} to {@code target} at a fixed speed, orbiting its own travel
     * axis (the orbit tightens to nothing as it nears the portal so it lands dead on its target), drawing a teal dust
     * mote plus a faint end-rod trail each tick and humming softly along the way.
     */
    private static final class Streamer {

        private final Vector origin;
        private final Vector axis;      // normalised origin -> target
        private final Vector perpA;     // orbit plane basis
        private final Vector perpB;
        private final double length;    // origin -> target distance
        private final double speed;     // blocks per tick
        private final double spinRadius;
        private final double spinSpeed; // radians per tick (signed for direction)

        private double traveled = 0;
        private int age = 0;

        private Streamer(Vector origin, Vector target, ThreadLocalRandom random) {
            this.origin = origin;
            final Vector delta = target.clone().subtract(origin);
            delta.subtract(delta.clone().normalize().multiply(2));
            this.length = delta.length();
            this.axis = delta.clone().multiply(1.0 / Math.max(length, 1.0e-6));

            // Any vector not parallel to the axis gives us a stable orbit plane.
            Vector reference = Math.abs(axis.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
            this.perpA = axis.getCrossProduct(reference).normalize();
            this.perpB = axis.getCrossProduct(perpA).normalize();

            this.speed = random.nextDouble(0.7, 1.1);
            this.spinRadius = random.nextDouble(0.8, 1.8);
            this.spinSpeed = random.nextDouble(0.3, 0.6) * (random.nextBoolean() ? 1 : -1);
        }

        /**
         * Advances one tick and renders. Returns {@code true} once the comet has reached the portal (and played its
         * impact), signalling the owner to drop it.
         */
        private boolean advance(World world) {
            traveled += speed;
            final double progress = Math.min(traveled / length, 1.0);

            final Vector point = origin.clone().add(axis.clone().multiply(Math.min(traveled, length)));

            if (progress >= 1.0) {
                final Location impact = point.toLocation(world);
                Particle.SCULK_CHARGE_POP.builder()
                        .location(impact)
                        .count(50)
                        .extra(0.1)
                        .receivers(VIEW_RANGE)
                        .spawn();
                new SoundEffect(Sound.BLOCK_DRIED_GHAST_PLACE_IN_WATER, (float) (0.3F + Math.random() * 0.5f), 2.4f).play(impact);
                return true;
            }

            // Orbit tightens to zero at the portal so the line converges precisely on the target.
            final double radius = spinRadius * (1 - progress);
            final double angle = age * spinSpeed;
            point.add(perpA.clone().multiply(Math.cos(angle) * radius));
            point.add(perpB.clone().multiply(Math.sin(angle) * radius));

            final Location loc = point.toLocation(world);
            Particle.SQUID_INK.builder()
                    .location(loc)
                    .count(1)
                    .offset(0.1, 0.1, 0.1)
                    .extra(0)
//                    .data(new Particle.DustTransition(Color.fromRGB(0x404040), Color.BLACK, 2f))
                    .receivers(VIEW_RANGE)
                    .spawn();

            new SoundEffect(Sound.ENTITY_BLAZE_BURN, 0.2F, 0.2f).play(loc);
            age++;
            return false;
        }
    }
}
