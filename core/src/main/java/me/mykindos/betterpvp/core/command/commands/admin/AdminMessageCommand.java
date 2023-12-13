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

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@Singleton
public class AdminMessageCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public AdminMessageCommand(ClientManager clientManager){
        this.clientManager = clientManager;

        aliases.add("ma");
    }

    @Override
    public String getName() {
        return "messageadmin";
    }

    @Override
    public String getDescription() {
        return "Send an admin message to a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();

        if (args.length == 0) {
            UtilMessage.message(player, "Core", "Usage: <player> <message>");
            return;
        }

        if (args.length == 1) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }

        final Collection<Client> matches = clientManager.search(player).advancedOnline(args[0]);
        if (matches.size() != 1) {
            return; /// no matches or too many matches - inform is done by the search
        }


        final Client receiver = matches.iterator().next();
        final Gamer receiverGamer = receiver.getGamer();
        final Player receiverPlayer = Objects.requireNonNull(receiverGamer.getPlayer());

        String playerName = UtilFormat.spoofNameForLunar(player.getName());
        Rank sendRank = client.getRank();
        Rank receiveRank = receiver.getRank();

        gamer.setLastAdminMessenger(receiver.getUuid());
        receiverGamer.setLastAdminMessenger(gamer.getUuid());

        Component senderComponent = sendRank.getPlayerNameMouseOver(playerName);
        Component receiverComponent = receiveRank.getPlayerNameMouseOver(receiver.getName());
        Component arrow = Component.text(" -> ", NamedTextColor.DARK_PURPLE);
        Component message = Component.text(" " + String.join(" ", Arrays.stream(args).toList().subList(1, args.length)), NamedTextColor.LIGHT_PURPLE);
        // Start with a Component.empty() to avoid the hoverEvent from propagating down
        Component component = Component.empty().append(senderComponent).append(arrow).append(receiverComponent).append(message);

        if (!receiver.hasRank(Rank.HELPER)) {
            // Don't send the message twice to a staff member
            UtilMessage.message(receiverPlayer, component);
        }

        clientManager.sendMessageToRank("", component, Rank.HELPER);
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }


}
