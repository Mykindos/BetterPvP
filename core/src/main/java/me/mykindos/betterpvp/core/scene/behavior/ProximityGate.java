package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Runs another behaviour only while somebody is close enough for it to matter.
 * <p>
 * {@code SceneTicker} already skips objects whose chunk has unloaded, but a loaded chunk nobody is standing in still
 * ticks - and a world full of props each throwing particles for an empty room adds up fast, multiplied again by every
 * instanced copy of that world. This closes that gap.
 * <p>
 * The nearby check is itself the expense, so it is answered once every {@link #CHECK_INTERVAL} ticks and reused in
 * between. A behaviour that must not miss a tick should not be wrapped.
 */
public class ProximityGate implements SceneBehavior {

    private static final int CHECK_INTERVAL = 20;

    private final SceneObject owner;
    private final double radiusSquared;
    private final SceneBehavior delegate;

    private int sinceCheck = CHECK_INTERVAL;
    private boolean anyoneNear;

    public ProximityGate(@NotNull SceneObject owner, double radius, @NotNull SceneBehavior delegate) {
        this.owner = owner;
        this.radiusSquared = radius * radius;
        this.delegate = delegate;
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void tick() {
        if (++sinceCheck >= CHECK_INTERVAL) {
            sinceCheck = 0;
            anyoneNear = anyoneNear();
        }
        if (anyoneNear) {
            delegate.tick();
        }
    }

    private boolean anyoneNear() {
        if (!owner.isMaterialized()) {
            return false;
        }

        final Location location = owner.getEntity().getLocation();
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }
}
