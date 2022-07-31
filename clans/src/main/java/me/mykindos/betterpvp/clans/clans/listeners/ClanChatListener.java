package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class ClanChatListener implements Listener {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    public ClanChatListener(ClanManager clanManager, ClientManager clientManager) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChatReceived(ChatReceivedEvent event) {
        Optional<Clan> targetClanOptional = clanManager.getClanByPlayer(event.getTarget());
        Optional<Clan> senderClanOptional = clanManager.getClanByPlayer(event.getPlayer());

        if (targetClanOptional.isPresent() && senderClanOptional.isPresent()) {
            Clan targetClan = targetClanOptional.get();
            Clan senderClan = senderClanOptional.get();

            ClanRelation relation = clanManager.getRelation(senderClan, targetClan);

            Component clanPrefix = Component.text(senderClan.getName() + " ", relation.getSecondary())
                    .append(Component.text(event.getPlayer().getName() + ": ", relation.getPrimary()));

            event.setPrefix(clanPrefix);
        } else {
            event.setPrefix(Component.text(event.getPlayer().getName() + ": ", NamedTextColor.YELLOW));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChatSent(ChatSentEvent event) {
        if (event.isCancelled()) return;

        Optional<Client> clientOptional = clientManager.getObject(event.getPlayer().getUniqueId().toString());
        if (clientOptional.isEmpty()) return;

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(event.getPlayer());
        if (clanOptional.isEmpty()) return;

        Client client = clientOptional.get();
        Clan clan = clanOptional.get();

        Optional<Boolean> clanChatEnabledOptional = client.getProperty(GamerProperty.CLAN_CHAT);
        clanChatEnabledOptional.ifPresent(clanChat -> {
            if (clanChat) {
                event.setCancelled(true, "Player has clan chat enabled");

                String message = ChatColor.AQUA + event.getPlayer().getName() + " "
                        + ChatColor.DARK_AQUA + PlainTextComponentSerializer.plainText().serialize(event.getMessage());
                clan.messageClan(message, null, false);

            }
        });

        Optional<Boolean> allyChatEnabledOptional = client.getProperty(GamerProperty.ALLY_CHAT);
        allyChatEnabledOptional.ifPresent(allyChat -> {
            if (allyChat) {
                event.setCancelled(true, "Player has ally chat enabled");

                String message = ChatColor.DARK_GREEN + event.getPlayer().getName() + " "
                        + ChatColor.GREEN + PlainTextComponentSerializer.plainText().serialize(event.getMessage());

                clan.getAlliances().forEach(alliance -> {
                    Optional<Clan> allianceOptional = clanManager.getObject(alliance.getOtherClan());
                    allianceOptional.ifPresent(allyClan -> allyClan.messageClan(message, null, false));
                });

                clan.messageClan(message, null, false);
            }
        });

    }
}
