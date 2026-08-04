package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data;

import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * A single shard of bone that bursts out of the ground, holds, then sinks back down and removes
 * itself. Ticked by whoever spawned it.
 */
public class BoneSpike {

    private static final float BURIED = -1.1f;
    private static final float SURFACED = -0.5f;
    private static final float SPEED = 0.4f;

    private final ItemDisplay display;
    private final Vector3f translation;
    private final int lingerTicks;

    private int surfacedTicks;
    private boolean retracting;
    @Getter
    private boolean finished;

    public BoneSpike(Location location, int lingerTicks) {
        this.lingerTicks = lingerTicks;

        // Whole blocks read as chunks of bone shouldered out of the earth, single bones as jagged shards
        final boolean chunk = UtilMath.RANDOM.nextDouble() < 0.3;
        final Location spawnLocation = location.clone();
        spawnLocation.setYaw(UtilMath.RANDOM.nextFloat() * 360f);
        spawnLocation.setPitch(0);

        // The bone item's sprite runs diagonally, so roll it upright to stand vertical
        final AxisAngle4f upright = chunk ? new AxisAngle4f() : new AxisAngle4f((float) Math.toRadians(45), 0, 0, 1);
        this.translation = new Vector3f(0, BURIED, 0);
        this.display = spawnLocation.getWorld().spawn(spawnLocation, ItemDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setItemStack(new ItemStack(chunk ? Material.BONE_BLOCK : Material.BONE));
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            spawned.setBrightness(new Display.Brightness(15, 15));
            spawned.setTransformation(new Transformation(
                    new Vector3f(translation),
                    new AxisAngle4f(),
                    new Vector3f(1f),
                    upright));
        });
    }

    public void tick() {
        if (finished) {
            return;
        }

        if (!display.isValid()) {
            finished = true;
            return;
        }

        if (retracting) {
            translation.y -= SPEED;
            if (translation.y <= BURIED) {
                remove();
                return;
            }
        } else if (translation.y < SURFACED) {
            translation.y = Math.min(SURFACED, translation.y + SPEED);
            if (translation.y >= SURFACED) {
                breakSurface();
            }
        } else if (++surfacedTicks >= lingerTicks) {
            retracting = true;
        }

        applyTranslation();
    }

    private void breakSurface() {
        final Location location = display.getLocation();
        new SoundEffect(Sound.BLOCK_BONE_BLOCK_BREAK, 0.6f + UtilMath.RANDOM.nextFloat() * 0.6f, 0.4f).play(location);
        Particle.BLOCK.builder()
                .location(location)
                .data(Material.BONE_BLOCK.createBlockData())
                .offset(0.2, 0.1, 0.2)
                .count(8)
                .extra(0)
                .allPlayers()
                .spawn();
    }

    private void applyTranslation() {
        final Transformation current = display.getTransformation();
        display.setTransformation(new Transformation(
                new Vector3f(translation),
                current.getLeftRotation(),
                current.getScale(),
                current.getRightRotation()));
    }

    public void remove() {
        if (display.isValid()) {
            display.remove();
        }
        finished = true;
    }
}
