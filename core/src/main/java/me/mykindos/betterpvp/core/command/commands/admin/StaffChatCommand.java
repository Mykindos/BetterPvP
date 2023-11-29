package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class StaffChatCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public StaffChatCommand(ClientManager clientManager){
        this.clientManager = clientManager;

        aliases.add("sc");
    }

    @Override
    public String getName() {
        return "staffchat";
    }

    @Override
    public String getDescription() {
        return "Toggle staff only chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();
        if(args.length > 0) {
            String playerName = UtilFormat.spoofNameForLunar(player.getName());
            Rank sendRank = client.getRank();
            Component senderComponent = sendRank.getPlayerNameMouseOver(playerName);
            Component message = Component.text(" " + String.join(" ", args), NamedTextColor.LIGHT_PURPLE);
            //Start with a Component.empty() to avoid the hoverEvent from propagating down
            Component component = Component.empty().append(senderComponent).append(message);

            clientManager.sendMessageToRank("", component, Rank.HELPER);
            return;
        }
        boolean staffChatEnabled = true;

        Optional<Boolean> staffChatEnabledOptional = client.getProperty(ClientProperty.STAFF_CHAT);
        if(staffChatEnabledOptional.isPresent()){
            staffChatEnabled = !staffChatEnabledOptional.get();
        }

        client.saveProperty(ClientProperty.STAFF_CHAT, staffChatEnabled);
        gamer.saveProperty(GamerProperty.ALLY_CHAT, false);
        gamer.saveProperty(GamerProperty.CLAN_CHAT, false);

        Component result = Component.text((staffChatEnabled ? "enabled" : "disabled"), (staffChatEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        UtilMessage.simpleMessage(player, "Command", Component.text("Staff Chat: ").append(result));
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }


}
