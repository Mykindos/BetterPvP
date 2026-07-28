package me.mykindos.betterpvp.core.scene.effect;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * A one-shot audible or visible cue that can be played anywhere.
 * <p>
 * An effect knows what to show and not where: the origin is supplied by whatever fires it, so the same
 * parsed effect serves a model bone, a fixed marker, or an entity's feet. Always played on the main
 * thread by its caller.
 */
@FunctionalInterface
public interface SceneEffect {

    void play(@NotNull Location origin);
}
