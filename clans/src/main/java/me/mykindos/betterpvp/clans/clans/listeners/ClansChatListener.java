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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

        String playerName = UtilFormat.spoofNameForLunar(event.getPlayer().getName());

        if (senderClanOptional.isPresent()) {
            Clan senderClan = senderClanOptional.get();

            ClanRelation relation = clanManager.getRelation(senderClan, targetClan);

            Component clanPrefix = Component.empty()
                    .append(Component.text(senderClan.getName() + " ", relation.getSecondary()))
                    .append(Component.text(playerName + ": ", relation.getPrimary()));


            event.setPrefix(clanPrefix);
        } else {
            event.setPrefix(Component.text(playerName  + ": ", NamedTextColor.YELLOW));
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

                clan.clanChat(event.getPlayer(), PlainTextComponentSerializer.plainText().serialize(event.getMessage()));

            }
        });

        Optional<Boolean> allyChatEnabledOptional = gamer.getProperty(GamerProperty.ALLY_CHAT);
        allyChatEnabledOptional.ifPresent(allyChat -> {
            if (allyChat) {
                event.cancel("Player has ally chat enabled");

                clan.allyChat(event.getPlayer(), PlainTextComponentSerializer.plainText().serialize(event.getMessage()));
            }
        });

    }
}
