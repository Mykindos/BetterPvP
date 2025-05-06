package me.mykindos.betterpvp.core.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageReceivedEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.UUID;

@BPvPListener
@Singleton
@CustomLog
public class AdminChatListener implements Listener {

    private final Core core;
    private final ClientManager clientManager;

    @Inject
    public AdminChatListener(Core core, ClientManager clientManager) {
        this.core = core;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onMessageReceived(MineplexMessageReceivedEvent event) {
        if (!event.getMessage().getChannel().equalsIgnoreCase("AdminMessage")) return;

        HashMap<String, String> metadata = event.getMessage().getMetadata();
        String sender = metadata.get("sender");
        if (sender == null) return;

        clientManager.search().offline(UUID.fromString(sender)).thenAccept(clientOptional -> {
            if (clientOptional.isEmpty()) return;

            Client client = clientOptional.get();

            String playerName = UtilFormat.spoofNameForLunar(client.getName());
            Rank sendRank = client.getRank();
            Component senderComponent = Component.empty();
            if (event.getMessage().getServer() != null) {
                senderComponent = Component.text(event.getMessage().getServer() + " ", NamedTextColor.WHITE);
            }
            senderComponent = senderComponent.append(Component.text(playerName, sendRank.getColor()).hoverEvent(HoverEvent.showText(Component.text(sendRank.getName(), sendRank.getColor()))));
            Component message = Component.text(" " + event.getMessage().getMessage(), NamedTextColor.LIGHT_PURPLE);
            //Start with a Component.empty() to avoid the hoverEvent from propagating down
            Component component = Component.empty().append(senderComponent).append(message);
            if (!client.hasRank(Rank.HELPER)) {
                //dont send the message twice to a staff member
                Player player = client.getGamer().getPlayer();
                if (player != null) {
                    UtilMessage.message(player, component);
                    UtilMessage.message(player, "Core", "If a staff member is on this server, they have received this message");
                }
            }

            clientManager.sendMessageToRank("", component, Rank.HELPER);
        }).exceptionally(ex -> {
            log.error("Failed processing admin message", ex);
            return null;
        });


    }
}
