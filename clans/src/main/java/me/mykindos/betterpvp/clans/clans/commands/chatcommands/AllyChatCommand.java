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
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class AllyChatCommand extends Command {

    private final Clans clans;
    private final ClanManager clanManager;

    @Inject
    public AllyChatCommand(Clans clans, ClanManager clanManager){
        this.clans = clans;
        this.clanManager = clanManager;

        aliases.add("ac");
    }

    @Override
    public String getName() {
        return "allychat";
    }

    @Override
    public String getDescription() {
        return "Toggle ally only chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You must be in a Clan to send an Ally Message");
            return;
        }
        Clan clan = clanOptional.get();

        if (args.length > 0) {
            UtilServer.callEventAsync(clans, new ChatSentEvent(player, clan.getAllianceChatChannel(), Component.text(UtilFormat.spoofNameForLunar(player.getName()) + ": "),
                    Component.text(String.join(" ", args))));
            return;
        }

        if (gamer.getChatChannel().equals(clan.getAllianceChatChannel())) {
            gamer.setChatChannel(ChatChannel.SERVER);
        } else {
            gamer.setChatChannel(ChatChannel.ALLIANCE);
        }
    }
}
