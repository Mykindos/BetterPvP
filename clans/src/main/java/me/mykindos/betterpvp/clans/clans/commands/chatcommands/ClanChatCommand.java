package me.mykindos.betterpvp.clans.clans.commands.chatcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ClanChatCommand extends Command {

    private final GamerManager gamerManager;

    @Inject
    public ClanChatCommand(GamerManager gamerManager){
        this.gamerManager = gamerManager;

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
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if(gamerOptional.isPresent()) {
            boolean clanChatEnabled = true;
            Gamer gamer = gamerOptional.get();
            Optional<Boolean> clanChatEnabledOptional = gamer.getProperty(GamerProperty.CLAN_CHAT);
            if(clanChatEnabledOptional.isPresent()){
                clanChatEnabled = !clanChatEnabledOptional.get();
            }

            gamer.putProperty(GamerProperty.CLAN_CHAT, clanChatEnabled);
            gamer.putProperty(GamerProperty.ALLY_CHAT, false);
            UtilMessage.message(player, "Command", "Clan Chat: "
                    + (clanChatEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        }
    }
}
