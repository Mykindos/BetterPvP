package me.mykindos.betterpvp.core.chat;

import com.google.inject.Inject;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.discord.DiscordMessage;
import me.mykindos.betterpvp.core.discord.DiscordWebhook;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
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
public class ChatListener implements Listener {

    @Inject
    @Config(path = "discord.chatWebhook", defaultValue = "")
    private String discordChatWebhook;

    private final ClientManager clientManager;

    private final GamerManager gamerManager;

    @Inject
    public ChatListener(ClientManager clientManager, GamerManager gamerManager) {
        this.clientManager = clientManager;
        this.gamerManager = gamerManager;
    }

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

        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
        if (gamerOptional.isEmpty()) return;

        Gamer gamer = gamerOptional.get();

        String playerName = UtilFormat.spoofNameForLunar(event.getPlayer().getName());

        Optional<Boolean> staffChatEnabledOptional = gamer.getProperty(GamerProperty.STAFF_CHAT);
        staffChatEnabledOptional.ifPresent(staffChat -> {
            if (staffChat) {
                event.cancel("Player has staff chat enabled");

                Rank sendRank = gamer.getClient().getRank();
                Component senderComponent = sendRank.getPlayerNameMouseOver(playerName);
                Component message = Component.text(" " + PlainTextComponentSerializer.plainText().serialize(event.getMessage()), NamedTextColor.LIGHT_PURPLE);
                Component component = Component.empty().append(senderComponent).append(message);

                gamerManager.sendMessageToRank("", component, Rank.HELPER);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinishSendingChat(ChatSentEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Optional<Client> clientOptional = clientManager.getObject(event.getPlayer().getUniqueId());
        if(clientOptional.isPresent()) {

            Client client = clientOptional.get();

            for (Player onlinePlayer : event.getTargets()) {
                ChatReceivedEvent chatReceived = new ChatReceivedEvent(player, client, onlinePlayer, event.getPrefix(), event.getMessage());
                Bukkit.getPluginManager().callEvent(chatReceived);
                if (chatReceived.isCancelled()) {
                    log.info("ChatReceivedEvent cancelled for {} - {}", onlinePlayer.getName(), event.getCancelReason());
                }
            }

        }else{
            log.error("ChatReceivedEvent could not be called as the sending client does not exist - {}", event.getPlayer().getName());
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatReceived(ChatReceivedEvent event) {
        if (event.isCancelled()) return;

        Rank rank = event.getClient().getRank();
        if(rank.isDisplayPrefix()) {
            Component rankPrefix = Component.text(rank.getName() + " ", rank.getColor(), TextDecoration.BOLD);
            event.setPrefix(rankPrefix.append(event.getPrefix().decoration(TextDecoration.BOLD, false)));
        }

        Optional<Boolean> lunarClientOptional = event.getClient().getProperty(ClientProperty.LUNAR);
        if(lunarClientOptional.isPresent()) {
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
