package me.mykindos.betterpvp.core.scene.npc;

import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * What an NPC does when it is right-clicked, looked up by name.
 * <p>
 * This is what keeps map data and features apart: a resident marked {@code interact:trade} does not
 * know what trading is, and trading does not know which villager it is attached to. A feature claims a
 * name here, a builder types that name on a marker, and the two meet without either owning the other -
 * so moving the broker, or having two of them, is a map edit rather than a code change.
 */
@CustomLog
@Singleton
public class NpcInteractionRegistry {

    private final Map<String, Consumer<Player>> interactions = new HashMap<>();

    /**
     * Claims an interaction name. Registering the same name twice replaces the first, which makes this
     * safe to call again on a reload.
     */
    public void register(@NotNull String id, @NotNull Consumer<Player> action) {
        interactions.put(id.toLowerCase(Locale.ROOT), action);
    }

    @Nullable
    public Consumer<Player> get(@NotNull String id) {
        return interactions.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Runs the named interaction for a player.
     *
     * @return {@code false} if no feature has claimed that name, which usually means a typo on a marker.
     */
    public boolean run(@NotNull String id, @NotNull Player player) {
        final Consumer<Player> action = get(id);
        if (action == null) {
            log.warn("NPC declares interaction '{}', which nothing has registered", id).submit();
            return false;
        }

        action.accept(player);
        return true;
    }
}
