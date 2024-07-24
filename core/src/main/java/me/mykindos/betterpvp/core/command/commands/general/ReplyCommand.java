package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@CustomLog
@Singleton
public class ReplyCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public ReplyCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
        aliases.add("r");
    }

    @Override
    public String getName() {
        return "Reply";
    }

    @Override
    public String getDescription() {
        return "Reply to another players message";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Command", "Usage: /reply <message>");
            return;
        }

        Optional<UUID> lastMessagedOptional = client.getProperty(ClientProperty.LAST_MESSAGED.name());
        if(lastMessagedOptional.isEmpty()){
            UtilMessage.message(player, "Command", "You have nobody to reply to.");
            return;
        }

        UUID lastMessaged = lastMessagedOptional.get();
        Player target = Bukkit.getPlayer(lastMessaged);
        if(target == null) {
            UtilMessage.message(player, "Command", "Player not found.");
            return;
        }

        Client targetClient = clientManager.search().online(target);
        // Todo check if client has targetClient ignored
        // TODO check if targetClient has sender ignored, and if they do, make it look like the message was sent successfully

        String message = String.join(" ", args);

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

        log.info(player.getName() + " messaged " + target.getName() + ": " + message);
    }
}
