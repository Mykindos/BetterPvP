package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Turns the object on the spot at a constant rate, for anything that should read as running rather than parked - a
 * windmill, a floating relic, a ship's wheel drifting in the swell.
 * <p>
 * It rotates the backing entity, which carries any attached model with it, and never moves the object - so its anchor
 * stays put and chunk-managed materialization keeps working.
 */
public class SpinBehavior implements SceneBehavior {

    private final SceneObject owner;
    private final float degreesPerTick;

    private float yaw;

    /**
     * @param degreesPerSecond how far it turns each second; negative spins the other way
     */
    public SpinBehavior(@NotNull SceneObject owner, double degreesPerSecond) {
        this.owner = owner;
        this.degreesPerTick = (float) (degreesPerSecond / 20.0);
    }

    @Override
    public void start() {
        if (owner.isMaterialized()) {
            yaw = owner.getEntity().getLocation().getYaw();
        }
    }

    @Override
    public void tick() {
        if (!owner.isMaterialized()) {
            return;
        }

        yaw = (yaw + degreesPerTick) % 360f;
        final Location location = owner.getEntity().getLocation();
        owner.getEntity().setRotation(yaw, location.getPitch());
    }
}
