package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.events.PlayerChangeChatChannelEvent;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
@Singleton
public class ClansChatListener extends ClanListener {

    @Inject
    public ClansChatListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChatReceived(ChatReceivedEvent event) {
        Clan targetClan = clanManager.getClanByPlayer(event.getTarget()).orElse(null);
        Clan senderClan = clanManager.getClanByPlayer(event.getPlayer()).orElse(null);

        String playerName = UtilFormat.spoofNameForLunar(event.getPlayer().getName());

        if (event.getChannel() == ChatChannel.SERVER) {

            if (senderClan != null) {

                ClanRelation relation = clanManager.getRelation(senderClan, targetClan);

                Component clanPrefix = Component.empty()
                        .append(Component.text(senderClan.getName() + " ", relation.getSecondary()))
                        .append(Component.text(playerName + ": ", relation.getPrimary()));


                event.setPrefix(clanPrefix);
            } else {
                event.setPrefix(Component.text(playerName + ": ", NamedTextColor.YELLOW));
            }
        } else if (event.getChannel() == ChatChannel.ALLIANCE) {
            if (senderClan != null) {
                event.setPrefix(Component.text(senderClan.getName() + " " + playerName + " ", NamedTextColor.DARK_GREEN));
                event.setMessage(event.getMessage().color(NamedTextColor.GREEN));
            }
        } else if (event.getChannel() == ChatChannel.CLAN) {
            if (senderClan != null) {
                event.setPrefix(Component.text(playerName + " ", NamedTextColor.AQUA));
                event.setMessage(event.getMessage().color(NamedTextColor.DARK_AQUA));
            }
        }
    }

    @EventHandler
    public void onPlayerChangeChatChannel(PlayerChangeChatChannelEvent event) {
        if (event.getTargetChannel() == ChatChannel.CLAN || event.getTargetChannel() == ChatChannel.ALLIANCE) {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(event.getGamer().getPlayer());
            if (clanOptional.isPresent()) {
                event.setNewChannel(event.getTargetChannel() == ChatChannel.CLAN ? clanOptional.get().getClanChatChannel() : clanOptional.get().getAllianceChatChannel());
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMemberKicked(ClanKickMemberEvent event) {
        clientManager.search().offline(event.getClanMember().getUuid()).thenAccept(clientOptional -> {
            Client target = clientOptional.orElseThrow();
            target.getGamer().setChatChannel(ChatChannel.SERVER);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMemberLeave(MemberLeaveClanEvent event) {
        clientManager.search().online(event.getPlayer()).getGamer().setChatChannel(ChatChannel.SERVER);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisband(ClanDisbandEvent event) {
        event.getClan().getMembers().forEach(member -> clientManager.search().offline(member.getUuid()).thenAcceptAsync(client -> {
            client.ifPresent(value -> value.getGamer().setChatChannel(ChatChannel.SERVER));
        }));
    }
}
