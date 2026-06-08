package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDController;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDItem;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDManager;
import me.mykindos.betterpvp.core.locale.Translations;
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
        return "search";
    }

    @Override
    public String getDescription() {
        return "core.command.search.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "core.prefix.search", "core.command.search.usage");
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
        return "core.command.search-item.description";
    }

        public Component getUsage() {
            return Translations.component("core.command.search.item.usage");
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "core.prefix.search", getUsage());
                return;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
            } catch (IllegalArgumentException e) {
                UtilMessage.message(player, "core.prefix.search", "core.command.search.item.invalid_uuid", Component.text(args[0]));
                return;
            }

            Optional<UUIDItem> uuidItemOptional = uuidManager.getObject(uuid.toString());

            if (uuidItemOptional.isEmpty()) {
                UtilMessage.message(player, "core.prefix.search", "core.command.search.item.not_found", Component.text(uuid.toString()));
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
        return "core.command.search-player.description";
    }

        public Component getUsage() {
            return Translations.component("core.command.search.player.usage");
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "core.prefix.search", getUsage());
                return;
            }

            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
                clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
                    if (clientOptional.isEmpty()) {
                        UtilMessage.message(player, "core.prefix.search", "core.command.search.player.invalid_player", Component.text(args[0]));
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
        UUIDController uuidController;

        @Override
        public String getName() {
            return "online";
        }

        @Override
        public String getDescription() {
        return "core.command.search-online.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            clientManager.sendMessageToRank("core.prefix.search", Translations.component("core.command.search.online.retrieving",
                    Component.text(player.getName())), Rank.TRIAL_MOD);

            boolean atLeastOneUUIDItemFound = false;
            for (Player online : Bukkit.getOnlinePlayers()) {
                boolean hasAtLeastOneUUIDItem = false;
                Component component = Translations.component("core.command.search.online.holding", Component.text(online.getName()));
                for (UUIDItem uuidItem : uuidController.getUUIDItems(online).keySet()) {
                    if (!hasAtLeastOneUUIDItem) {
                        hasAtLeastOneUUIDItem = true;
                    }
                    component = component.appendNewline().append(Translations.component("core.command.search.online.entry",
                            Component.text(uuidItem.getIdentifier()),
                            Component.text(uuidItem.getUuid().toString())));
                }
                if (hasAtLeastOneUUIDItem) {
                    atLeastOneUUIDItemFound = true;
                    UtilMessage.message(player, "core.prefix.search", component);
                }

            }
            if (!atLeastOneUUIDItemFound) {
                //there are no UUIDItems being held
                UtilMessage.message(player, "core.prefix.search", "core.command.search.online.none");
            }
        }

        @Override
        public String getArgumentType(int argCount) {
            return ArgumentType.NONE.name();
        }
    }
}