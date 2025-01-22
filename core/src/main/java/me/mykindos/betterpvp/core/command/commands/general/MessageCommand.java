package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@Singleton
@CustomLog
public class MessageCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public MessageCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
        aliases.addAll(List.of("m", "msg", "tell", "whisper", "w"));
    }

    @Override
    public String getName() {
        return "message";
    }

    @Override
    public String getDescription() {
        return "Send a message to another player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length > 1) {

            if (client.hasPunishment(PunishmentTypes.MUTE)) {
                UtilMessage.message(player, "Command", "You may not message other players while muted");
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if(target == null) {
                UtilMessage.message(player, "Command", "Player not found.");
                return;
            }



            if(player.equals(target)) {
                UtilMessage.message(player, "Command", "You cannot message yourself.");
                return;
            }

            Client targetClient = clientManager.search().online(target);
            // check if client has target igored
            if (client.ignoresClient(targetClient).join()) {
                UtilMessage.message(player, "Command", "You cannot message <yellow>%s</yellow>, you have them ignored!", target.getName());
                return;
            }

            if(!player.isListed(target) && !client.hasRank(Rank.ADMIN)) {
                UtilMessage.message(player, "Command", "Player not found.");
                return;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            // check if target has client ignored, if so, fake the message
            if (targetClient.ignoresClient(client).join()) {
                UtilMessage.simpleMessage(player, "<dark_aqua>[<aqua>You<dark_aqua> -> <aqua>" + target.getName() + "<dark_aqua>] <gray>" + message);
                client.putProperty(ClientProperty.LAST_MESSAGED.name(), target.getUniqueId(), true);
                return;
            }

            UtilMessage.simpleMessage(player, "<dark_aqua>[<aqua>You<dark_aqua> -> <aqua>" + target.getName() + "<dark_aqua>] <gray>" + message);
            UtilMessage.simpleMessage(target, "<dark_aqua>[<aqua>" + player.getName() + "<dark_aqua> -> <aqua>You<dark_aqua>] <gray>" + message);

            for(Player online : Bukkit.getOnlinePlayers()) {
                if(online.equals(target) || online.equals(player)) continue;
                if(clientManager.search().online(online).isAdministrating()) {
                    UtilMessage.simpleMessage(online, "<dark_green>[<green>" + player.getName() + "<dark_green> -> <green>" + target.getName() + "<dark_green>] <gray>" + message);
                }
            }

            client.putProperty(ClientProperty.LAST_MESSAGED.name(), target.getUniqueId(), true);
            targetClient.putProperty(ClientProperty.LAST_MESSAGED.name(), client.getUniqueId(), true);

            log.info("{} messaged {}: {}", player.getName(), target.getName(), message).submit();

        } else {
            UtilMessage.simpleMessage(player, "Command", "Usage: /message <player> <message>");
        }
    }

    @Override
    public String getArgumentType(int index) {
        return index == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
