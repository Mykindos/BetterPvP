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
public class AdminMessageCommand extends Command {

    private final GamerManager gamerManager;

    @Inject
    public AdminMessageCommand(GamerManager gamerManager){
        this.gamerManager = gamerManager;

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
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if(gamerOptional.isPresent()) {
            Gamer gamer = gamerOptional.get();
            Gamer receivingGamer = null;
            if(args.length == 0) {
                UtilMessage.message(player, "Core", "Usage: <player> <message>");
                return;
            }
            if(args.length == 1) {
                UtilMessage.message(player, "Core", "You must specify a message");
                return;
            }
            Optional<Gamer> optionalReceivingGamer = gamerManager.getGamerByName(args[0]);
            Player receiver = null;
            if (optionalReceivingGamer.isPresent()) {

                receivingGamer = optionalReceivingGamer.get();
                receiver = receivingGamer.getPlayer();

            }
            if (receiver == null) {
                UtilMessage.message(player, "Core", UtilMessage.deserialize("<gray>No player named <yellow>" + args[0] + "<gray> found"));
                return;
            }

            String playerName = UtilFormat.spoofNameForLunar(player.getName());
            Rank sendRank = gamer.getClient().getRank();
            Rank receiveRank = receivingGamer.getClient().getRank();

            Component senderComponent = Component.text(playerName, sendRank.getColor()).hoverEvent(HoverEvent.showText(Component.text(sendRank.getName(), sendRank.getColor())));
            Component receiverComponent = Component.text(receiver.getName(), receiveRank.getColor()).hoverEvent(HoverEvent.showText(Component.text(receiveRank.getName(), receiveRank.getColor())));
            Component arrow = Component.text(" -> ", NamedTextColor.DARK_PURPLE);
            Component message = Component.text(" " + String.join(" ", Arrays.stream(args).toList().subList(1, args.length)), NamedTextColor.LIGHT_PURPLE);
            //Start with a Component.empty() to avoid the hoverEvent from propagating down
            Component component = Component.empty().append(senderComponent).append(arrow).append(receiverComponent).append(message);
            if (!receivingGamer.getClient().hasRank(Rank.HELPER)) {
                //dont send the message twice to a staff member
                UtilMessage.message(receiver, component);
            }

            gamerManager.sendMessageToRank("", component, Rank.HELPER);
        }
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
