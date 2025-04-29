package me.mykindos.betterpvp.clans.mineplex;

import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.messaging.MessagingModule;
import com.mineplex.studio.sdk.modules.messaging.event.AsyncMineplexMessageReceivedEvent;
import lombok.CustomLog;
import lombok.NonNull;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.mineplex.MineplexMessage;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
@PluginAdapter("StudioEngine")
@CustomLog
public class ClansMineplexMessageListener implements Listener {

    private final MessagingModule messagingModule;


    public ClansMineplexMessageListener() {
        this.messagingModule = MineplexModuleManager.getRegisteredModule(MessagingModule.class);
        this.messagingModule.registerKey("Clans");
    }

    @EventHandler
    public void onMessageReceived(final AsyncMineplexMessageReceivedEvent event) {
        Optional<@NonNull Object> messageOptional = event.getMessageIf("Clans");
        if (messageOptional.isPresent() && messageOptional.get() instanceof MineplexMessage message) {
            log.info("Received message: {}", message.getMessage()).submit();
        }
    }
}
