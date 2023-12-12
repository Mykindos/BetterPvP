package me.mykindos.betterpvp.clans.clans.commands.chatcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class ClanChatCommand extends Command {

    private final ClientManager clientManager;

    private final ClanManager clanManager;

    @Inject
    public ClanChatCommand(ClientManager clientManager, ClanManager clanManager){
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
        if(args.length > 0) {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            if (clanOptional.isEmpty()) {
                UtilMessage.message(player, "Clans", "You must be in a Clan to send a Clan Message");
                return;
            }
            Clan clan = clanOptional.get();
            clan.clanChat(player, String.join(" ", args));
            return;
        }
        boolean clanChatEnabled = true;
        Optional<Boolean> clanChatEnabledOptional = gamer.getProperty(GamerProperty.CLAN_CHAT);
        if(clanChatEnabledOptional.isPresent()){
            clanChatEnabled = !clanChatEnabledOptional.get();
        }

        gamer.saveProperty(GamerProperty.CLAN_CHAT, clanChatEnabled);
        gamer.saveProperty(GamerProperty.ALLY_CHAT, false);
        client.saveProperty(ClientProperty.STAFF_CHAT, false);

        Component result = Component.text((clanChatEnabled ? "enabled" : "disabled"), (clanChatEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        UtilMessage.simpleMessage(player, "Command", Component.text("Clan Chat: ").append(result));
    }
}
