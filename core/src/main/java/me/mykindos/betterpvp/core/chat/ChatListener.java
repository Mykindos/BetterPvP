package me.mykindos.betterpvp.core.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.discord.DiscordMessage;
import me.mykindos.betterpvp.core.discord.DiscordWebhook;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;
import java.util.Set;

@Slf4j
@BPvPListener
@Singleton
public class ChatListener implements Listener {

    @Inject
    @Config(path = "discord.chatWebhook")
    private String discordChatWebhook;

    @Inject
    private ClientManager clientManager;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {

        event.setCancelled(true);

        Player player = event.getPlayer();

        Component message = event.message().color(NamedTextColor.WHITE);
        message = message.decorations(Set.of(TextDecoration.values()), false);

        ChatSentEvent chatSent = new ChatSentEvent(player, Bukkit.getOnlinePlayers(), Component.text(UtilFormat.spoofNameForLunar(player.getName()) + ": "), message);
        Bukkit.getPluginManager().callEvent(chatSent);
        if (chatSent.isCancelled()) {
            log.info("ChatSentEvent cancelled for {} - {}", chatSent.getPlayer().getName(), chatSent.getCancelReason());
        }

        logChatToDiscord(event.getPlayer(), message);

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChatSent(ChatSentEvent event) {
        if (event.isCancelled()) return;

        Client client = clientManager.search().online(event.getPlayer());
        String playerName = UtilFormat.spoofNameForLunar(event.getPlayer().getName());

        Optional<Boolean> staffChatEnabledOptional = client.getProperty(ClientProperty.STAFF_CHAT);
        staffChatEnabledOptional.ifPresent(staffChat -> {
            if (staffChat) {
                event.cancel("Player has staff chat enabled");

                Rank sendRank = client.getRank();
                Component senderComponent = sendRank.getPlayerNameMouseOver(playerName);
                Component message = Component.text(" " + PlainTextComponentSerializer.plainText().serialize(event.getMessage()), NamedTextColor.LIGHT_PURPLE);
                Component component = Component.empty().append(senderComponent).append(message);

                clientManager.sendMessageToRank("", component, Rank.HELPER);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinishSendingChat(ChatSentEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Client client = clientManager.search().online(event.getPlayer());
        for (Player onlinePlayer : event.getTargets()) {
            ChatReceivedEvent chatReceived = new ChatReceivedEvent(player, client, onlinePlayer, event.getPrefix(), event.getMessage());
            Bukkit.getPluginManager().callEvent(chatReceived);
            if (chatReceived.isCancelled()) {
                log.info("ChatReceivedEvent cancelled for {} - {}", onlinePlayer.getName(), event.getCancelReason());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatReceived(ChatReceivedEvent event) {
        if (event.isCancelled()) return;

        Rank rank = event.getClient().getRank();
        if (rank.isDisplayPrefix()) {
            Component rankPrefix = Component.text(rank.getName() + " ", rank.getColor(), TextDecoration.BOLD);
            event.setPrefix(rankPrefix.append(event.getPrefix().decoration(TextDecoration.BOLD, false)));
        }

        Optional<Boolean> lunarClientOptional = event.getClient().getProperty(ClientProperty.LUNAR);
        if (lunarClientOptional.isPresent()) {
            event.setPrefix(Component.text("* ", NamedTextColor.GREEN).append(event.getPrefix()));
        }

        Component finalMessage = event.getPrefix().append(event.getMessage());
        event.getTarget().sendMessage(finalMessage);

    }

    private void logChatToDiscord(Player player, Component message) {
        if(!discordChatWebhook.isEmpty()){
            DiscordWebhook webhook = new DiscordWebhook(discordChatWebhook);
            webhook.send(DiscordMessage.builder()
                    .username(player.getName())
                    .messageContent(PlainTextComponentSerializer.plainText().serialize(message))
                    .build());
        }
    }


}
