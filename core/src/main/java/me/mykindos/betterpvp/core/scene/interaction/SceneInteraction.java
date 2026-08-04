package me.mykindos.betterpvp.core.scene.interaction;

import me.mykindos.betterpvp.core.scene.ScenePlacement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * What happens when a player right-clicks a scene object that names this action.
 * <p>
 * Any condition on the action - a quest not started, a ship already sailing, a rank the player does not have - belongs
 * inside {@link #run}, together with whatever it tells the player about it. The registry deliberately has no notion of
 * a refusal, because "you cannot do this" is always specific to the feature and always needs to say why.
 *
 * @see SceneInteractionRegistry
 */
@FunctionalInterface
public interface SceneInteraction {

    /**
     * @param player    who clicked
     * @param placement which marker was clicked - its id, its tags, and the world it is in
     */
    void run(@NotNull Player player, @NotNull ScenePlacement placement);
}
