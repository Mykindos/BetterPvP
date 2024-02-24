package me.mykindos.betterpvp.core.command.commands.admin.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.logger.UUIDItem;
import me.mykindos.betterpvp.core.items.logger.UUIDManager;
import me.mykindos.betterpvp.core.items.logger.UuidLogger;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
        UtilMessage.message(player, "Search", UtilMessage.deserialize("<green>Usage: /search <item|player></green>"));
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
                UtilMessage.message(player, "Search", UtilMessage.deserialize("<light_purple>%s</light_purple> is not a valid UUID.", args[0]));
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
                UtilMessage.message(player, "Search", UtilMessage.deserialize("There is no item with the UUID <light_purple>%s</light_purple>", uuid.toString()));
                return;
            }

            UUIDItem uuidItem = uuidItemOptional.get();
            clientManager.sendMessageToRank("Search", UtilMessage.deserialize("<yellow>%s</yellow> is retrieving logs for <light_purple>%s</light_purple> (<green>%s</green>)", player.getName(), uuid.toString(), uuidItem.getIdentifier()), Rank.HELPER);

            List<String> logs = UuidLogger.getUuidLogs(uuid, amount);
            UtilMessage.message(player, "Search", "Retrieving the last <green>%s</green> logs for <light_purple>%s</light_purple> (<green>%s</green>)", amount, uuid.toString(), uuidItem.getIdentifier());

            for (String log : logs) {
                UtilMessage.message(player, "Search", UtilMessage.deserialize("<white>" + log + "</white>"));
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

    @Singleton
    @SubCommand(SearchCommand.class)
    public static class SearchPlayerSubCommand extends Command {

        @Inject
        ClientManager clientManager;

        @Override
        public String getName() {
            return "player";
        }

        @Override
        public String getDescription() {
            return "Search for an item by a player";
        }

        public Component getUsage() {
            return UtilMessage.deserialize("<green>Usage: <player> [amount]");
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "Search", getUsage());
                return;
            }

            clientManager.search().offline(args[0], clientOptional -> {
                if (clientOptional.isEmpty()) {
                    UtilMessage.message(player, "Search", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid Player.", args[0]));
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

                Client client1 = clientOptional.get();
                clientManager.sendMessageToRank("Search", UtilMessage.deserialize("<yellow>%s</yellow> is retrieving logs for <yellow>%s</yellow>", player.getName(), client1.getName()), Rank.HELPER);

                List<String> logs = UuidLogger.getPlayerLogs(client1.getUniqueId(), amount);
                UtilMessage.message(player, "Search", "Retrieving the last <green>%s</green> logs for <yellow>%s</yellow>", amount, client1.getName());

                for (String log : logs) {
                    UtilMessage.message(player, "Search", UtilMessage.deserialize("<white>" + log + "</white>"));
                }
            });
        }

        @Override
        public String getArgumentType(int argCount) {
            if (argCount == 1) {
                return ArgumentType.PLAYER.name();
            }
            return ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(SearchCommand.class)
    public static class SearchOnlineSubCommand extends Command {

        @Inject
        ClientManager clientManager;

        @Inject
        ItemHandler itemHandler;

        @Override
        public String getName() {
            return "online";
        }

        @Override
        public String getDescription() {
            return "Return all UUID Items currently held by players";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
                clientManager.sendMessageToRank("Search", UtilMessage.deserialize("<yellow>%s</yellow> is retrieving all <light_purple>UUIDitems</light_purple> currently being held", player.getName()), Rank.HELPER);

                for (Player player1 : Bukkit.getOnlinePlayers()) {
                    Component component = UtilMessage.deserialize("<yellow>%s</yellow> is holding:", player1.getName());
                    for (UUIDItem uuidItem : itemHandler.getUUIDItems(player1)) {
                        component = component.appendNewline().append(UtilMessage.deserialize("(<green>%s</green>) <light_purple>%s</light_purple>", uuidItem.getIdentifier(), uuidItem.getUuid()));
                    }
                    UtilMessage.message(player, "Search", component);
                }
        }

        @Override
        public String getArgumentType(int argCount) {
            return ArgumentType.NONE.name();
        }
    }
}
