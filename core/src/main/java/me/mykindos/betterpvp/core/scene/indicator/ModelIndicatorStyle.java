package me.mykindos.betterpvp.core.scene.indicator;

import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.display.SceneModelDisplay;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An indicator drawn as a ModelEngine model, looping an idle clip — so the marker's own animation is its motion and
 * nothing has to move it about to make it look alive.
 */
public class ModelIndicatorStyle implements IndicatorStyle {

    private final String modelId;
    private final String idleAnimation;
    private final double scale;
    @Nullable private final Display.Billboard billboard;

    public ModelIndicatorStyle(@NotNull String modelId, @NotNull String idleAnimation) {
        this(modelId, idleAnimation, 1.0, null);
    }

    /**
     * @param billboard how the marker turns to face whoever is looking; {@code null} leaves it fixed the way the
     *                  blueprint built it
     */
    public ModelIndicatorStyle(@NotNull String modelId, @NotNull String idleAnimation, double scale,
                               @Nullable Display.Billboard billboard) {
        this.modelId = modelId;
        this.idleAnimation = idleAnimation;
        this.scale = scale;
        this.billboard = billboard;
    }

    @Override
    public @NotNull SceneObject create(@NotNull Location at) {
        // ModelEngine never renders the backing entity; it only follows it and rides its tracker - which is what lets
        // an indicator's visibility filter keep working unchanged. The same invisible pig every other modeled scene
        // object in the codebase is built on.
        final Entity base = at.getWorld().spawn(at, Pig.class, spawned -> {
            spawned.setAI(false);
            spawned.setInvulnerable(true);
            spawned.setCollidable(false);
            spawned.setPersistent(false);
            spawned.setInvisible(true);
            spawned.setSilent(true);
        });

        final SceneModelDisplay display = new SceneModelDisplay(modelId, idleAnimation, scale, billboard);
        display.init(base);
        return display;
    }
}
