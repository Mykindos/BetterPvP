package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Floats the object gently up and down, for things that should look weightless - a hovering rune, a lantern on a swell,
 * a relic that refuses to sit still.
 * <p>
 * The rest height is read from the entity when the behaviour starts, which is the anchor, so a chunk cycle puts the
 * object back exactly where the map placed it rather than wherever the wave happened to be.
 */
public class BobBehavior implements SceneBehavior {

    private final SceneObject owner;
    private final double amplitude;
    private final double radiansPerTick;

    private double restY;
    private double phase;
    private boolean anchored;

    /**
     * @param amplitude    how far it travels above and below its rest height, in blocks
     * @param periodSeconds how long one full up-and-down takes
     */
    public BobBehavior(@NotNull SceneObject owner, double amplitude, double periodSeconds) {
        this.owner = owner;
        this.amplitude = amplitude;
        this.radiansPerTick = (2 * Math.PI) / (Math.max(0.05, periodSeconds) * 20.0);
    }

    @Override
    public void tick() {
        if (!owner.isMaterialized()) {
            return;
        }

        final Location location = owner.getEntity().getLocation();
        if (!anchored) {
            restY = location.getY();
            anchored = true;
        }

        phase += radiansPerTick;
        location.setY(restY + Math.sin(phase) * amplitude);
        owner.getEntity().teleport(location);
    }
}
