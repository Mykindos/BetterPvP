package me.mykindos.betterpvp.clans.clans.commands.chatcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.ServerChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class ClanChatCommand extends Command {

    private final Clans clans;
    private final ClientManager clientManager;
    private final ClanManager clanManager;

    @Inject
    public ClanChatCommand(Clans clans, ClientManager clientManager, ClanManager clanManager) {
        this.clans = clans;
        this.clientManager = clientManager;
        this.clanManager = clanManager;

        aliases.add("cc");
    }

    @Override
    public String getName() {
        return "clanchat";
    }

    @Override
    public String getDescription() {
        return "Toggle clan only chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You must be in a Clan to send a Clan Message");
            return;
        }

        Clan clan = clanOptional.get();

        if (args.length > 0) {
            UtilServer.callEventAsync(clans, new ChatSentEvent(player, clan.getClanChatChannel(), Component.text(UtilFormat.spoofNameForLunar(player.getName()) + ": "),
                    Component.text(String.join(" ", args))));
            return;
        }

        if (gamer.getChatChannel().equals(clan.getClanChatChannel())) {
            gamer.setChatChannel(ChatChannel.SERVER);
        } else {
            gamer.setChatChannel(ChatChannel.CLAN);
        }
    }
}
