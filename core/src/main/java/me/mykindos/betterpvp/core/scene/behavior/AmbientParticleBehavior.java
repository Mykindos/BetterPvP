package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;
import org.bukkit.util.Vector;

/**
 * Emits a puff of particles at the object on a fixed beat - smoke off a chimney, embers over a forge, motes around a
 * shrine.
 * <p>
 * Wrap this in a {@link ProximityGate} for anything decorative: particles nobody can see still cost a tick and a packet
 * per viewer in range.
 */
public class AmbientParticleBehavior implements SceneBehavior {

    private final SceneObject owner;
    private final Particle particle;
    private final int count;
    private final double spread;
    private final Vector offset;
    private final int intervalTicks;

    private int sinceLast;

    /**
     * @param spread        random distribution around the emit point, in blocks
     * @param offset        where the emit point sits relative to the object's feet
     * @param intervalTicks ticks between puffs; a value below 1 is treated as every tick
     */
    public AmbientParticleBehavior(@NotNull SceneObject owner, @NotNull Particle particle, int count, double spread,
                                   @NotNull Vector offset, int intervalTicks) {
        this.owner = owner;
        this.particle = particle;
        this.count = count;
        this.spread = spread;
        this.offset = offset;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.sinceLast = this.intervalTicks;
    }

    @Override
    public void tick() {
        if (++sinceLast < intervalTicks || !owner.isMaterialized()) {
            return;
        }
        sinceLast = 0;

        final Location at = owner.getEntity().getLocation().add(offset);
        at.getWorld().spawnParticle(particle, at, count, spread, spread, spread, 0);
    }
}
