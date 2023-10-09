package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Optional;

@Singleton
public class PlayerAdminCommand extends Command {

    private final GamerManager gamerManager;

    @Inject
    public PlayerAdminCommand(GamerManager gamerManager){
        this.gamerManager = gamerManager;

        aliases.add("a");
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription() {
        return "Send an admin message to a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if(gamerOptional.isPresent()) {
            Gamer gamer = gamerOptional.get();
            if(args.length == 0) {
                UtilMessage.message(player, "Core", "You must specify a message");
                return;
            }
            String playerName = UtilFormat.spoofNameForLunar(player.getName());
            Rank sendRank = gamer.getClient().getRank();
            Component senderComponent = Component.text(playerName, sendRank.getColor()).hoverEvent(HoverEvent.showText(Component.text(sendRank.getName(), sendRank.getColor())));
            Component message = Component.text(" " + String.join(" ", args), NamedTextColor.LIGHT_PURPLE);
            //Start with a Component.empty() to avoid the hoverEvent from propagating down
            Component component = Component.empty().append(senderComponent).append(message);
            if (!gamer.getClient().hasRank(Rank.HELPER)) {
                //dont send the message twice to a staff member
                UtilMessage.message(player, component);
                UtilMessage.message(player, "Core", "If a staff member is on this server, they have received this message");
            }

            gamerManager.sendMessageToRank("", component, Rank.HELPER);
        }
    }
}
