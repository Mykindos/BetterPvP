package me.mykindos.betterpvp.core.framework.mineplex;

import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.messaging.MessagingModule;
import com.mineplex.studio.sdk.modules.messaging.event.AsyncMineplexMessageReceivedEvent;
import com.mineplex.studio.sdk.modules.messaging.target.MineplexMessageTarget;
import com.mineplex.studio.sdk.util.NamespaceUtil;
import lombok.CustomLog;
import lombok.NonNull;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageReceivedEvent;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageSentEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
@PluginAdapter("StudioEngine")
@CustomLog
public class MineplexMessageListener implements Listener {

    private static final String NAMESPACE = "65448710242da44445143493_668d617e14f04f2d5c90d717";

    private final MessagingModule messagingModule;


    public MineplexMessageListener() {
        this.messagingModule = MineplexModuleManager.getRegisteredModule(MessagingModule.class);
        this.messagingModule.registerKey("BetterPvP");
    }

    @EventHandler
    public void onMessageReceived(final AsyncMineplexMessageReceivedEvent event) {

        Optional<@NonNull Object> messageOptional = event.getMessageIf("BetterPvP");
        if (messageOptional.isPresent() && messageOptional.get() instanceof MineplexMessage message) {
            UtilServer.callEvent(new MineplexMessageReceivedEvent(message.getChannel(), message));
            log.info("Received message: {}", message.getMessage()).submit();
        }
    }

    @EventHandler
    public void onMessageSent(final MineplexMessageSentEvent event) {
        event.getMessage().setServer(NamespaceUtil.getCommonName());
        messagingModule.sendMessage("BetterPvP", event.getMessage(), MineplexMessageTarget.matchingNamespace(NAMESPACE));
    }
}
