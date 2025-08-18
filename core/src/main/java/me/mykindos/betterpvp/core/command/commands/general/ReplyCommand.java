package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
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
    private final IFilterService filterService;
    private final IIgnoreService ignoreService;

    @Inject
    public ReplyCommand(ClientManager clientManager, IFilterService filterService, IIgnoreService ignoreService) {
        this.clientManager = clientManager;
        this.filterService = filterService;
        this.ignoreService = ignoreService;
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

        if (client.hasPunishment(PunishmentTypes.MUTE)) {
            UtilMessage.message(player, "Command", "You may not message other players while muted");
            return;
        }

        Optional<UUID> lastMessagedOptional = client.getProperty(ClientProperty.LAST_MESSAGED.name());
        if (lastMessagedOptional.isEmpty()) {
            UtilMessage.message(player, "Command", "You have nobody to reply to.");
            return;
        }

        UUID lastMessaged = lastMessagedOptional.get();
        Player target = Bukkit.getPlayer(lastMessaged);
        if (target == null) {
            UtilMessage.message(player, "Command", "Player not found.");
            return;
        }

        Client targetClient = clientManager.search().online(target);

        if (!player.isListed(target) && !client.hasRank(Rank.ADMIN)) {
            UtilMessage.message(player, "Command", "Player not found.");
            return;
        }

        String message = String.join(" ", args);
        filterService.filterMessage(message).thenAccept(filteredMessage -> {
            boolean isClientIgnored = ignoreService.isClientIgnored(targetClient, client);
            boolean isTargetIgnored = ignoreService.isClientIgnored(client, targetClient);

            if (isTargetIgnored) {
                UtilMessage.message(player, "Command", "You cannot message <yellow>%s</yellow>, you have them ignored!", target.getName());
                return;
            }

            if (isClientIgnored) {
                // We still send a fake message
                UtilMessage.simpleMessage(player, "<dark_aqua>[<aqua>You<dark_aqua> -> <aqua>" + target.getName() + "<dark_aqua>] <gray>" + filteredMessage);
                client.putProperty(ClientProperty.LAST_MESSAGED.name(), target.getUniqueId(), true);
                return;
            }


            UtilMessage.simpleMessage(player, "<dark_aqua>[<aqua>You<dark_aqua> -> <aqua>" + target.getName() + "<dark_aqua>] <gray>" + filteredMessage);
            UtilMessage.simpleMessage(target, "<dark_aqua>[<aqua>" + player.getName() + "<dark_aqua> -> <aqua>You<dark_aqua>] <gray>" + filteredMessage);

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(target) || online.equals(player)) continue;
                if (clientManager.search().online(online).isAdministrating()) {
                    UtilMessage.simpleMessage(online, "<dark_green>[<green>" + player.getName() + "<dark_green> -> <green>" + target.getName() + "<dark_green>] <gray>" + filteredMessage);
                }
            }

            client.putProperty(ClientProperty.LAST_MESSAGED.name(), target.getUniqueId(), true);
            targetClient.putProperty(ClientProperty.LAST_MESSAGED.name(), client.getUniqueId(), true);

            log.info(player.getName() + " messaged " + target.getName() + ": " + message).submit();

        });

    }
}
