package me.mykindos.betterpvp.core.scene.interaction;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.scene.ScenePlacement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What a scene object does when it is right-clicked, looked up by name.
 * <p>
 * This is what keeps map data and features apart: a marker tagged {@code interact:trade} does not know what trading is,
 * and trading does not know which villager - or which ship's helm - it is attached to. A feature claims a name here, a
 * builder types that name on a marker, and the two meet without either owning the other, so moving the broker, or
 * having ten of them across ten worlds, is a map edit rather than a code change.
 * <p>
 * Names are shared by every kind of scene object. A resident and a prop tagged {@code interact:trade} both open the
 * broker; nothing about an action is specific to being an NPC.
 */
@CustomLog
@Singleton
public class SceneInteractionRegistry {

    private final Map<String, SceneInteraction> interactions = new HashMap<>();

    /**
     * Claims an interaction name. Registering the same name twice replaces the first, which makes this safe to call
     * again on a reload.
     */
    public void register(@NotNull String id, @NotNull SceneInteraction action) {
        interactions.put(id.toLowerCase(Locale.ROOT), action);
    }

    @Nullable
    public SceneInteraction get(@NotNull String id) {
        return interactions.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Runs the named interaction.
     *
     * @return {@code false} if no feature has claimed that name, which usually means a typo on a marker
     */
    public boolean run(@NotNull String id, @NotNull Player player, @NotNull ScenePlacement placement) {
        final SceneInteraction action = get(id);
        if (action == null) {
            log.warn("Scene object at {} declares interaction '{}', which nothing has registered",
                    placement.getLocation(), id).submit();
            return false;
        }

        action.run(player, placement);
        return true;
    }
}
