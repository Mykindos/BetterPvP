package me.mykindos.betterpvp.clans.clans.commands.chatcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

            gamer.saveProperty(GamerProperty.CLAN_CHAT, clanChatEnabled);
            gamer.saveProperty(GamerProperty.ALLY_CHAT, false);
            gamer.saveProperty(GamerProperty.STAFF_CHAT, false);

            Component result = Component.text((clanChatEnabled ? "enabled" : "disabled"), (clanChatEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            UtilMessage.simpleMessage(player, "Command", Component.text("Clan Chat: ").append(result));
        }
    }
}
