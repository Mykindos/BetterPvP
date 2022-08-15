package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
public class ClansChatListener extends ClanListener {

    @Inject
    public ClansChatListener(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChatReceived(ChatReceivedEvent event) {
        Clan targetClan = clanManager.getClanByPlayer(event.getTarget()).orElse(null);
        Optional<Clan> senderClanOptional = clanManager.getClanByPlayer(event.getPlayer());

        if (senderClanOptional.isPresent()) {
            Clan senderClan = senderClanOptional.get();

            ClanRelation relation = clanManager.getRelation(senderClan, targetClan);

            //HoverEvent<Component> clanTooltip = HoverEvent.showText(clanManager.getClanTooltip(event.getTarget(), senderClan));
            Component clanPrefix = Component.empty()
                    .append(Component.text(senderClan.getName() + " ", relation.getSecondary()))
                    .append(Component.text(event.getPlayer().getName() + ": ", relation.getPrimary()));


            event.setPrefix(clanPrefix);
        } else {
            event.setPrefix(Component.text(event.getPlayer().getName() + ": ", NamedTextColor.YELLOW));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChatSent(ChatSentEvent event) {
        if (event.isCancelled()) return;

        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
        if (gamerOptional.isEmpty()) return;

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(event.getPlayer());
        if (clanOptional.isEmpty()) return;

        Gamer gamer = gamerOptional.get();
        Clan clan = clanOptional.get();

        Optional<Boolean> clanChatEnabledOptional = gamer.getProperty(GamerProperty.CLAN_CHAT);
        clanChatEnabledOptional.ifPresent(clanChat -> {
            if (clanChat) {

                event.cancel("Player has clan chat enabled");

                String message = ChatColor.AQUA + event.getPlayer().getName() + " " + ChatColor.DARK_AQUA + PlainTextComponentSerializer.plainText().serialize(event.getMessage());
                clan.messageClan(message, null, false);

            }
        });

        Optional<Boolean> allyChatEnabledOptional = gamer.getProperty(GamerProperty.ALLY_CHAT);
        allyChatEnabledOptional.ifPresent(allyChat -> {
            if (allyChat) {
                event.cancel("Player has ally chat enabled");

                String message = ChatColor.DARK_GREEN + event.getPlayer().getName() + " " + ChatColor.GREEN + PlainTextComponentSerializer.plainText().serialize(event.getMessage());

                clan.getAlliances().forEach(alliance -> {
                    alliance.getClan().messageClan(message, null, false);
                });

                clan.messageClan(message, null, false);
            }
        });

    }
}
