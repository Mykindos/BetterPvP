package me.mykindos.betterpvp.core.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.ServerChatChannel;
import me.mykindos.betterpvp.core.chat.channels.StaffChatChannel;
import me.mykindos.betterpvp.core.chat.channels.events.PlayerChangeChatChannelEvent;
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
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Set;

@CustomLog
@BPvPListener
@Singleton
public class ChatListener implements Listener {

    @Inject
    @Config(path = "discord.chatWebhook")
    private String discordChatWebhook;

    private final Core core;
    private final ClientManager clientManager;
    private final IFilterService filterService;

    @Inject
    public ChatListener(Core core, ClientManager clientManager, IFilterService filterService) {
        this.core = core;
        this.clientManager = clientManager;
        this.filterService = filterService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {

        event.setCancelled(true);

        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);

        Component message = event.message().color(NamedTextColor.WHITE);
        message = message.decorations(Set.of(TextDecoration.values()), false);

        ChatSentEvent chatSent = new ChatSentEvent(player, client.getGamer().getChatChannel(), Component.text(UtilFormat.spoofNameForLunar(player.getName()) + ": "), message);
        Bukkit.getPluginManager().callEvent(chatSent);
        if (chatSent.isCancelled()) {
            log.info("ChatSentEvent cancelled for {} - {}", chatSent.getPlayer().getName(), chatSent.getCancelReason()).submit();
        }

        logChatToDiscord(event.getPlayer(), message);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinishSendingChat(ChatSentEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Client client = clientManager.search().online(event.getPlayer());

        filterService.filterMessage(event.getMessage()).thenAccept(filteredMessage -> {
            for (Player onlinePlayer : event.getChannel().getAudience()) {
                ChatReceivedEvent chatReceived = UtilServer.callEvent(new ChatReceivedEvent(player, client, onlinePlayer, event.getChannel().getChannel(), event.getPrefix(), filteredMessage));
                if (chatReceived.isCancelled()) {
                    log.info("ChatReceivedEvent cancelled for {} - {}", onlinePlayer.getName(), event.getCancelReason()).submit();
                }
            }
        }).exceptionally(throwable -> {
            log.error("Error filtering message", throwable).submit();
            return null;
        });

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onStaffChat(ChatReceivedEvent event) {
        if(event.getChannel() != ChatChannel.STAFF) return;

        event.setPrefix(Component.text(event.getClient().getName() + " ", event.getClient().getRank().getColor()));
        event.setMessage(event.getMessage().color(NamedTextColor.LIGHT_PURPLE));
    }

    @EventHandler(ignoreCancelled = true)
    public void onChatFromIgnore(ChatReceivedEvent event) {
        final Client sender = event.getClient();
        final Client receiver = clientManager.search().online(event.getTarget());
        if (receiver.ignoresClient(sender).join()) {
            event.setCancelled(true);
            event.setCancelReason(receiver.getName() + " ignores " + sender.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatDisabled(ChatReceivedEvent event) {
        if (event.isCancelled()) return;
        if (event.getChannel() != ChatChannel.SERVER) return;
        Client client = clientManager.search().online(event.getTarget());
        if (!((boolean) client.getProperty(ClientProperty.CHAT_ENABLED).orElse(false))) {
            if (!event.getClient().hasRank(Rank.HELPER)) {
                event.setCancelled(true);
                event.setCancelReason("Player has chat disabled");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatReceived(ChatReceivedEvent event) {
        if (event.isCancelled()) return;


        if (event.getChannel() == ChatChannel.SERVER) {
            Component rankPrefix = event.getClient().getTag(true);
            String mediaChannel = (String) event.getClient().getProperty(ClientProperty.MEDIA_CHANNEL).orElse("");
            if (!mediaChannel.isEmpty()) {
                rankPrefix = rankPrefix.clickEvent(ClickEvent.openUrl(mediaChannel));
            }
            event.setPrefix(rankPrefix.append(event.getPrefix().decoration(TextDecoration.BOLD, false)));
        }

        Component finalMessage = event.getPrefix().append(event.getMessage());
        event.getTarget().sendMessage(finalMessage);

    }

    @EventHandler
    public void onChangeChatChannel(PlayerChangeChatChannelEvent event) {
        if (event.isCancelled()) return;
        if(event.getTargetChannel() == ChatChannel.SERVER) {
            event.setNewChannel(ServerChatChannel.getInstance());
        } else if(event.getTargetChannel() == ChatChannel.STAFF) {
            event.setNewChannel(new StaffChatChannel(clientManager));
        }

    }

    @EventHandler
    public void onSignEdit(final SignChangeEvent event) {
        // Since a sign is essentially one message, we want to filter all its lines together.
        final StringBuilder messageBuilder = new StringBuilder();
        for (final Component line : event.lines()) {
            // Since Signs can have component lines, we have to serialize each line into plain text to filter it.
            messageBuilder.append(' ').append(PlainTextComponentSerializer.plainText().serialize(line));
        }
        // Since the SignChangeEvent is fired on the main thread, we have to check the filter asynchronously.
        filterService.filterMessage(messageBuilder.toString()).thenAccept(filtered -> {
            // If the line wasn't filtered, we don't need to do anything
            UtilServer.runTask(core, () -> {
                // Update the sign to clear its lines
                if (event.getBlock().getState() instanceof Sign sign) {
                    SignSide side = sign.getSide(event.getSide());
                    final Component cleared = Component.text("");
                    // Minecraft signs have only 4 lines
                    for (int i = 0; i < 4; i++) {
                        side.line(i, cleared);
                    }
                    // We want to update the sign without triggering a game physics update
                    sign.update(false, false);
                }
            });
        }).exceptionally(throwable -> {
            log.error("Error filtering sign message", throwable).submit();
            return null;
        });
    }

    private void logChatToDiscord(Player player, Component message) {
        if (!discordChatWebhook.isEmpty()) {
            DiscordWebhook webhook = new DiscordWebhook(discordChatWebhook);
            webhook.send(DiscordMessage.builder()
                    .username(player.getName())
                    .messageContent(PlainTextComponentSerializer.plainText().serialize(message))
                    .build());
        }
    }


}
