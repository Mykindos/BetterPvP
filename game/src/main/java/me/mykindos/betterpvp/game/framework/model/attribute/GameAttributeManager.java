package me.mykindos.betterpvp.game.framework.model.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.event.PreGameChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages game attributes that can be set or retrieved via commands.
 */
@Singleton
@CustomLog
@BPvPListener
public class GameAttributeManager implements Listener {

    private final Map<String, GameAttribute<?>> attributes = new HashMap<>();
    private final ServerController serverController;

    @Inject
    public GameAttributeManager(ServerController serverController) {
        this.serverController = serverController;
    }

    /**
     * Registers a game attribute.
     * @param attribute The attribute to register
     */
    public void registerAttribute(GameAttribute<?> attribute) {
        attributes.put(attribute.getKey().toLowerCase(), attribute);
    }

    /**
     * Gets a game attribute by name.
     * @param name The name of the attribute
     * @return The attribute, or null if not found
     */
    @Nullable
    public GameAttribute<?> getAttribute(String name) {
        return attributes.get(name.toLowerCase());
    }

    /**
     * Gets all registered attributes.
     * @return A collection of all attributes
     */
    public Collection<GameAttribute<?>> getAttributes() {
        return attributes.values();
    }

    @EventHandler
    public void onGameChange(PreGameChangeEvent event) {
        // Trash all bound attributes before the next game is setup
        attributes.values().removeIf(BoundAttribute.class::isInstance);
    }
}
