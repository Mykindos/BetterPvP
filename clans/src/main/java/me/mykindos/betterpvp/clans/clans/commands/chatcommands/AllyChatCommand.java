package me.mykindos.betterpvp.clans.clans.commands.chatcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class AllyChatCommand extends Command {

    private final GamerManager gamerManager;

    private final ClanManager clanManager;

    @Inject
    public AllyChatCommand(GamerManager gamerManager, ClanManager clanManager){
        this.gamerManager = gamerManager;
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
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if(gamerOptional.isPresent()) {
            if(args.length > 0) {
                Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
                if (clanOptional.isEmpty()) {
                    UtilMessage.message(player, "Clans", "You must be in a Clan to send an Ally Message");
                    return;
                }
                Clan clan = clanOptional.get();
                clan.allyChat(player, String.join(" ", args));
                return;
            }
            boolean allyChatEnabled = true;
            Gamer gamer = gamerOptional.get();
            Optional<Boolean> allyChatEnabledOptional = gamer.getProperty(GamerProperty.ALLY_CHAT);
            if(allyChatEnabledOptional.isPresent()){
                allyChatEnabled = !allyChatEnabledOptional.get();
            }

            gamer.saveProperty(GamerProperty.ALLY_CHAT, allyChatEnabled);
            gamer.saveProperty(GamerProperty.CLAN_CHAT, false);
            gamer.saveProperty(GamerProperty.STAFF_CHAT, false);

            Component result = Component.text((allyChatEnabled ? "enabled" : "disabled"), (allyChatEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            UtilMessage.simpleMessage(player, "Command", Component.text("Ally Chat: ").append(result));
        }
    }
}
