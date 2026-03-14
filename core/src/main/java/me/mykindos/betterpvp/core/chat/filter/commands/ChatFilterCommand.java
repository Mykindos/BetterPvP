package me.mykindos.betterpvp.core.chat.filter.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.chat.filter.impl.DatabaseFilterService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class ChatFilterCommand extends Command {

    private final IFilterService filterService;

    @Inject
    public ChatFilterCommand(IFilterService filterService) {
        this.filterService = filterService;
    }

    @Override
    public String getName() {
        return "chatfilter";
    }

    @Override
    public String getDescription() {
        return "Manage general chat filter words";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Command", UtilMessage.deserialize("<gray>Usage: /chatfilter <add | remove | list | reload></gray>"));
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }


    @Singleton
    @SubCommand(ChatFilterCommand.class)
    public static class ChatFilterAddCommand extends Command {
        private final IFilterService filterService;

        @Inject
        public ChatFilterAddCommand(IFilterService filterService) {
            this.filterService = filterService;
        }

        @Override
        public String getName() {
            return "add";
        }

        @Override
        public String getDescription() {
            return "Add a word to the chat filter list";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "Filter", UtilMessage.deserialize("<green>Usage: /chatfilter add <word></green>"));
                return;
            }

            String word = String.join(" ", args).toLowerCase();

            filterService.addFilteredWord(word).thenAccept(success -> {
                if (success) {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("Added <yellow>%s</yellow> to the chat filter list", word));
                } else {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("<yellow>%s</yellow> is already in the chat filter list", word));
                }
            });
        }
    }

    @Singleton
    @SubCommand(ChatFilterCommand.class)
    public static class ChatFilterRemoveCommand extends Command {
        private final IFilterService filterService;

        @Inject
        public ChatFilterRemoveCommand(IFilterService filterService) {
            this.filterService = filterService;
        }

        @Override
        public String getName() {
            return "remove";
        }

        @Override
        public String getDescription() {
            return "Remove a word from the chat filter list";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "Filter", UtilMessage.deserialize("<green>Usage: /chatfilter remove <word></green>"));
                return;
            }

            String word = args[0].toLowerCase();

            filterService.removeFilteredWord(word).thenAccept(success -> {
                if (success) {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("Removed <yellow>%s</yellow> from the chat filter list", word));
                } else {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("<yellow>%s</yellow> is not in the chat filter list", word));
                }
            });
        }
    }

    @Singleton
    @SubCommand(ChatFilterCommand.class)
    public static class ChatFilterListCommand extends Command {
        private final IFilterService filterService;

        @Inject
        public ChatFilterListCommand(IFilterService filterService) {
            this.filterService = filterService;
        }

        @Override
        public String getName() {
            return "list";
        }

        @Override
        public String getDescription() {
            return "List all chat filtered words";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            Set<String> filteredWords = filterService.getFilteredWords();

            if (filteredWords.isEmpty()) {
                UtilMessage.message(player, "Filter", "There are no words in the chat filter list");
                return;
            }

            Component message = Component.text("Chat Filtered Words:", NamedTextColor.GREEN);
            List<String> sortedWords = new ArrayList<>(filteredWords);
            sortedWords.sort(String::compareTo);

            for (String word : sortedWords) {
                message = message.appendNewline().append(Component.text("- ", NamedTextColor.GRAY))
                        .append(Component.text(word, NamedTextColor.YELLOW));
            }

            UtilMessage.message(player, "Filter", message);
        }
    }

    @Singleton
    @SubCommand(ChatFilterCommand.class)
    public static class ChatFilterReloadCommand extends Command {
        private final IFilterService filterService;

        @Inject
        public ChatFilterReloadCommand(IFilterService filterService) {
            this.filterService = filterService;
        }

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the chat filter list from the database";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (filterService instanceof DatabaseFilterService databaseFilterService) {
                databaseFilterService.loadFilteredWords();
                UtilMessage.message(player, "Filter", "Reloaded chat filter list from database");
            }
        }
    }
}
