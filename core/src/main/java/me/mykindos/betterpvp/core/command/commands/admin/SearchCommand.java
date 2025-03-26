package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class SearchCommand extends Command {
    @Override
    public String getName() {
        return "legacysearch";
    }

    @Override
    public String getDescription() {
        return "Base Search command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Search", UtilMessage.deserialize("<green>Usage: /search <item|player></green>"));
    }

    @Singleton
    @SubCommand(SearchCommand.class)
    public static class SearchItemSubCommand extends Command {
        private final UUIDManager uuidManager;
        private final LogRepository logRepository;

        @Inject
        public SearchItemSubCommand(UUIDManager uuidManager, LogRepository logRepository) {
            this.uuidManager = uuidManager;
            this.logRepository = logRepository;
        }

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

            Optional<UUIDItem> uuidItemOptional = uuidManager.getObject(uuid);

            if (uuidItemOptional.isEmpty()) {
                UtilMessage.message(player, "Search", UtilMessage.deserialize("There is no item with the UUID <light_purple>%s</light_purple>", uuid.toString()));
                return;
            }

            UUIDItem uuidItem = uuidItemOptional.get();

            new CachedLogMenu(uuidItem.getIdentifier(), LogContext.ITEM, uuid.toString(), "ITEM_", CachedLogMenu.ITEM, JavaPlugin.getPlugin(Core.class), logRepository, null).show(player);

        }

        @Override
        public String getArgumentType(int argCount) {
            if (argCount == 1) {
                return "ITEMUUID";
            }
            return ArgumentType.NONE.name();
        }

        @Override
        public List<String> processTabComplete(CommandSender sender, String[] args) {
            List<String> tabCompletions = new ArrayList<>();

            if (args.length == 0) return super.processTabComplete(sender, args);

            String lowercaseArg = args[args.length - 1].toLowerCase();
            if (getArgumentType(args.length).equals("ITEMUUID")) {
                tabCompletions.addAll(uuidManager.getObjects().keySet().stream()
                        .filter(uuid -> uuid.toLowerCase().contains(lowercaseArg)).toList());
            }
            tabCompletions.addAll(super.processTabComplete(sender, args));
            return tabCompletions;
        }
    }

    @Singleton
    @SubCommand(SearchCommand.class)
    public static class SearchPlayerSubCommand extends Command {

        private final ClientManager clientManager;
        private final LogRepository logRepository;

        @Inject
        public SearchPlayerSubCommand(ClientManager clientManager, LogRepository logRepository) {
            this.clientManager = clientManager;
            this.logRepository = logRepository;
        }

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

            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
                clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
                    if (clientOptional.isEmpty()) {
                        UtilMessage.message(player, "Search", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid Player.", args[0]));
                        return;
                    }

                    Client targetClient = clientOptional.get();
                    CachedLogMenu cachedLogMenu = new CachedLogMenu(targetClient.getName(), LogContext.CLIENT, targetClient.getUniqueId().toString(), "ITEM_", CachedLogMenu.ITEM, JavaPlugin.getPlugin(Core.class), logRepository, null);
                    UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                        cachedLogMenu.show(player);
                    });
                });
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

            boolean atLeastOneUUIDItemFound = false;
            for (Player online : Bukkit.getOnlinePlayers()) {
                boolean hasAtLeastOneUUIDItem = false;
                Component component = UtilMessage.deserialize("<yellow>%s</yellow> is holding:", online.getName());
                for (UUIDItem uuidItem : itemHandler.getUUIDItems(online)) {
                    if (!hasAtLeastOneUUIDItem) {
                        hasAtLeastOneUUIDItem = true;
                    }
                    component = component.appendNewline().append(UtilMessage.deserialize("(<green>%s</green>) <light_purple>%s</light_purple>", uuidItem.getIdentifier(), uuidItem.getUuid()));
                }
                if (hasAtLeastOneUUIDItem) {
                    atLeastOneUUIDItemFound = true;
                    UtilMessage.message(player, "Search", component);
                }

            }
            if (!atLeastOneUUIDItemFound) {
                //there are no UUIDItems being held
                UtilMessage.message(player, "Search", "There are currently no UUIDItems being held");
            }
        }

        @Override
        public String getArgumentType(int argCount) {
            return ArgumentType.NONE.name();
        }
    }
}
