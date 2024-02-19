package me.mykindos.betterpvp.core.command.commands.admin.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.items.logger.UUIDItem;
import me.mykindos.betterpvp.core.items.logger.UUIDManager;
import me.mykindos.betterpvp.core.items.logger.UuidLogger;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class SearchCommand extends Command {
    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "Base Search command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
    }

    @Override
    public String getArgumentType(int argCount) {
        return ArgumentType.SUBCOMMAND.name();
    }

    @Singleton
    @SubCommand(SearchCommand.class)
    public static class SearchItemSubCommand extends Command {

        @Inject
        UUIDManager uuidManager;

        @Inject
        ClientManager clientManager;

        @Override
        public String getName() {
            return "item";
        }

        @Override
        public String getDescription() {
            return "Search for an item by its uuid";
        }

        public Component getUsage() {
            return UtilMessage.deserialize("<green>Usage: <UUID> [amount]");
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "Search", getUsage());
                return;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
            } catch (IllegalArgumentException e) {
                UtilMessage.message(player, "Search", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid UUID.", args[0]));
                return;
            }

            int amount = 5;

            if (args.length > 1) {
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount < 1) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    UtilMessage.message(player, "Search", UtilMessage.deserialize("<green>%s</green> is not a valid integer. Integer must be >= 1.", args[1]));
                    return;
                }
            }

            Optional<UUIDItem> uuidItemOptional = uuidManager.getObject(uuid);

            if (uuidItemOptional.isEmpty()) {
                UtilMessage.message(player, "Search", UtilMessage.deserialize("There is no item with the UUID <yellow>%s</yellow>", uuid.toString()));
                return;
            }

            UUIDItem uuidItem = uuidItemOptional.get();
            clientManager.sendMessageToRank("Search", UtilMessage.deserialize("<yellow>%s</yellow> is retrieving logs for <yellow>%s</yellow> (<green>%s</green>)", player.getName(), uuid.toString(), uuidItem.getIdentifier()), Rank.HELPER);

            List<String> logs = UuidLogger.getUuidLogs(uuid, amount);
            UtilMessage.message(player, "Search", "Retrieving the last <green>%s</green> logs for <yellow>%s</yellow> (<green>%s</green>)", amount, uuid.toString(), uuidItem.getIdentifier());

            for (String log : logs) {
                UtilMessage.message(player, "Search", UtilMessage.deserialize(log));
            }
        }

        @Override
        public String getArgumentType(int argCount) {
            if (argCount == 1) {
                return ArgumentType.ITEMUUID.name();
            }
            return ArgumentType.NONE.name();
        }
    }
}
