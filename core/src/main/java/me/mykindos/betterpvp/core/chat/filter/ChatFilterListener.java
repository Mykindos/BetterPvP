package me.mykindos.betterpvp.core.chat.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.stream.Collectors;

@CustomLog
@BPvPListener
@Singleton
public class ChatFilterListener implements Listener {

    private final ShadowChatFilterManager filterManager;
    private final ClientManager clientManager;

    @Inject
    public ChatFilterListener(ShadowChatFilterManager filterManager, ClientManager clientManager) {
        this.filterManager = filterManager;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onChatSent(ChatSentEvent event) {
        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);

        // Skip filtering for admins
        if (client != null && client.hasRank(Rank.ADMIN)) {
            return;
        }

        IChatChannel channel = event.getChannel();
        String channelName = channel.getChannel().name();

        if (channel.getChannel() != ChatChannel.SERVER) {
            return;
        }

        // Convert Component to String for filtering
        String messageText = PlainTextComponentSerializer.plainText().serialize(event.getMessage());

        // Check if message contains filtered words
        if (filterManager.containsFilteredWord(messageText)) {
            // Create a shadow channel that only sends to the original sender and staff
            event.setChannel(new ShadowChatChannel(clientManager, player));

            // Log the filtered message attempt
            log.info("[FILTERED] Player {} attempted to send a filtered message in {} channel: {}",
                    player.getName(), channelName, messageText).submit();

            clientManager.sendMessageToRank("Filter", Component.text("Player " + player.getName() + " attempted to send a filtered message in " + channelName + ": " + messageText), Rank.ADMIN);

        }
    }

    /**
     * A custom chat channel that only sends messages to the original sender and staff members
     */
    private static class ShadowChatChannel implements IChatChannel {

        private final ClientManager clientManager;
        private final Player sender;

        public ShadowChatChannel(ClientManager clientManager, Player sender) {
            this.clientManager = clientManager;
            this.sender = sender;
        }

        @Override
        public ChatChannel getChannel() {
            return ChatChannel.SERVER;
        }

        @Override
        public Collection<? extends Player> getAudience() {
            // Get all staff members (HELPER and above)
            Collection<Player> audience = clientManager.getOnline().stream()
                    .filter(client -> client.getGamer().getPlayer() != null && client.hasRank(Rank.ADMIN))
                    .map(client -> client.getGamer().getPlayer())
                    .collect(Collectors.toList());

            // Add the original sender if not already included
            if (!audience.contains(sender)) {
                audience.add(sender);
            }

            return audience;
        }
    }
}