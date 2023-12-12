package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class AdminReplyCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public AdminReplyCommand(ClientManager clientManager){
        this.clientManager = clientManager;

        aliases.add("ra");
    }

    @Override
    public String getName() {
        return "replyadmin";
    }

    @Override
    public String getDescription() {
        return "Send an admin message to a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Gamer gamer = client.getGamer();
        Client receivingClient = null;
        Gamer receivingGamer = null;
        if(args.length == 0) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }
        if (gamer.getLastAdminMessenger() == null) {
            UtilMessage.message(player, "Core", UtilMessage.deserialize("<gray>No previous player to reply to"));
            return;
        }

        Optional<Client> optionalReceiver = clientManager.search(player).online(UUID.fromString(gamer.getLastAdminMessenger()));
        Player receiver = null;
        if (optionalReceiver.isPresent()) {
            receivingClient = optionalReceiver.get();
            receivingGamer = receivingClient.getGamer();
            receiver = receivingGamer.getPlayer();
        }

        if (receiver == null) {
            UtilMessage.message(player, "Core", UtilMessage.deserialize("<gray>No online player to reply to found"));
            return;
        }

        String playerName = UtilFormat.spoofNameForLunar(player.getName());
        Rank sendRank = client.getRank();
        Rank receiveRank = receivingClient.getRank();

        gamer.setLastAdminMessenger(receivingGamer.getUuid());
        receivingGamer.setLastAdminMessenger(gamer.getUuid());

        Component senderComponent = sendRank.getPlayerNameMouseOver(playerName);
        Component receiverComponent = receiveRank.getPlayerNameMouseOver(receiver.getName());
        Component arrow = Component.text(" -> ", NamedTextColor.DARK_PURPLE);
        Component message = Component.text(" " + String.join(" ", args), NamedTextColor.LIGHT_PURPLE);
        //Start with a Component.empty() to avoid the hoverEvent from propagating down
        Component component = Component.empty().append(senderComponent).append(arrow).append(receiverComponent).append(message);
        if (!receivingClient.hasRank(Rank.HELPER)) {
            //dont send the message twice to a staff member
            UtilMessage.message(receiver, component);
        }

        clientManager.sendMessageToRank("", component, Rank.HELPER);
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }
}
