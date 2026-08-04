package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A cage of iron chains wrapped around a living entity - vertical bars around the perimeter of its
 * bounding box plus two horizontal bands - which follows the entity until its time runs out and then
 * snaps apart. Sized off the bounding box, so it fits anything from a player to a ravager.
 */
public class BattlebindChains {

    private static final double PADDING = 0.15;
    private static final double BAR_SPACING = 0.55;
    private static final int MIN_BARS = 6;
    private static final double[] BAND_HEIGHTS = {0.3, 0.7};

    private final LivingEntity target;
    private final List<BlockDisplay> chains = new ArrayList<>();
    private final long creationTime = System.currentTimeMillis();
    private final long duration;

    @Getter
    private boolean finished;

    public BattlebindChains(LivingEntity target, long duration) {
        this.target = target;
        this.duration = duration;

        final BoundingBox box = target.getBoundingBox();
        final double height = box.getHeight();
        final double radius = Math.max(box.getWidthX(), box.getWidthZ()) / 2 + PADDING;
        final int bars = Math.max(MIN_BARS, (int) Math.round(2 * Math.PI * radius / BAR_SPACING));

        for (int i = 0; i < bars; i++) {
            final double angle = 2 * Math.PI * i / bars;
            spawnSegment(new Vector(Math.cos(angle) * radius, height / 2, Math.sin(angle) * radius),
                    new Vector3f(0, 1, 0),
                    (float) height);
        }

        for (double band : BAND_HEIGHTS) {
            spawnBand(radius, height * band, bars);
        }

        new SoundEffect(Sound.BLOCK_CHAIN_PLACE, 0.8f, 1.2f).play(target.getLocation());
    }

    /**
     * A closed ring of chain at a given height, built from one chord per gap between the vertical bars.
     */
    private void spawnBand(double radius, double y, int segments) {
        for (int i = 0; i < segments; i++) {
            final double from = 2 * Math.PI * i / segments;
            final double to = 2 * Math.PI * (i + 1) / segments;
            final Vector start = new Vector(Math.cos(from) * radius, y, Math.sin(from) * radius);
            final Vector end = new Vector(Math.cos(to) * radius, y, Math.sin(to) * radius);

            final Vector chord = end.clone().subtract(start);
            final Vector3f direction = new Vector3f((float) chord.getX(), (float) chord.getY(), (float) chord.getZ());
            spawnSegment(start.clone().add(end).multiply(0.5), direction, (float) chord.length());
        }
    }

    /**
     * @param offset    where the centre of this chain sits relative to the entity's feet
     * @param direction which way the chain runs, in world space
     * @param length    how long the chain is, in blocks
     */
    private void spawnSegment(Vector offset, Vector3f direction, float length) {
        final Location spawnLocation = anchor();
        spawnLocation.setYaw(0);
        spawnLocation.setPitch(0);

        // With the display unrotated, its own frame is the world frame, so the whole orientation fits
        // in the left rotation and the centring offset only has to be pre-rotated by that same amount
        final Quaternionf rotation = new Quaternionf().rotateTo(new Vector3f(0, 1, 0), direction.normalize());
        final Vector3f translation = rotation.transform(new Vector3f(-0.5f, -length / 2f, -0.5f))
                .add((float) offset.getX(),
                        (float) (offset.getY() - target.getHeight()),
                        (float) offset.getZ());

        final BlockDisplay display = spawnLocation.getWorld().spawn(spawnLocation, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.IRON_CHAIN.createBlockData());
            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
            spawned.setTransformation(new Transformation(
                    translation,
                    rotation,
                    new Vector3f(1, length, 1),
                    new Quaternionf()));
        });

        chains.add(display);
    }

    private Location anchor() {
        return target.getLocation().add(0, target.getHeight(), 0);
    }

    public void tick() {
        if (finished) {
            return;
        }

        if (!target.isValid() || UtilTime.elapsed(creationTime, duration)) {
            release();
            return;
        }

        // Ridden rather than chased: a passenger is carried by the server along with its vehicle, so
        // knockback and teleports never leave the cage a tick behind. Teleporting ejects passengers,
        // so the mount is re-asserted rather than done once.
        for (BlockDisplay chain : chains) {
            if (chain.getVehicle() == null && !target.addPassenger(chain)) {
                chain.teleport(anchor());
            }
        }
    }

    /**
     * Snaps the cage apart and drops the chains.
     */
    public void release() {
        if (finished) {
            return;
        }
        finished = true;

        final Location location = target.getLocation();
        new SoundEffect(Sound.BLOCK_CHAIN_BREAK, 0.7f, 1.0f).play(location);
        Particle.BLOCK.builder()
                .location(location.clone().add(0, target.getHeight() / 2, 0))
                .data(Material.IRON_CHAIN.createBlockData())
                .offset(target.getWidth() / 2, target.getHeight() / 2, target.getWidth() / 2)
                .count(30)
                .extra(0)
                .allPlayers()
                .spawn();

        chains.forEach(BlockDisplay::remove);
        chains.clear();
    }
}
