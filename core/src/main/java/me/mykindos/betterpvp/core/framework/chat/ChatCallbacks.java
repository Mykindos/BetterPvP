package me.mykindos.betterpvp.core.framework.chat;

import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public final class ChatCallbacks {

    private final Map<UUID, Consumer<Component>> callbacks = new HashMap<>();

    public void listen(Player player, Consumer<Component> callback) {
        callbacks.put(player.getUniqueId(), callback);
    }

    public boolean execute(Player player, Component message) {
        Consumer<Component> callback = callbacks.remove(player.getUniqueId());
        if (callback != null) {
            callback.accept(message);
        }
        return callback != null;
    }

}
