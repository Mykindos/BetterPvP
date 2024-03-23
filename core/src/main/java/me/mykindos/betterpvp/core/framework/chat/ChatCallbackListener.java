package me.mykindos.betterpvp.core.framework.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@BPvPListener
@Singleton
public class ChatCallbackListener implements Listener {

    private final ChatCallbacks callbacks;

    @Inject
    private ChatCallbackListener(@NotNull ChatCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChat(final ChatSentEvent event) {
        if (callbacks.execute(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

}
