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
import me.mykindos.betterpvp.core.locale.Translations;
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
        return "core.command.chat-filter.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.usage");
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
        return "core.command.chat-filter-add.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.add.usage");
                return;
            }

            String word = String.join(" ", args).toLowerCase();

            filterService.addFilteredWord(word).thenAccept(success -> {
                if (success) {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.add.success",
                            Component.text(word, NamedTextColor.YELLOW));
                } else {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.add.exists",
                            Component.text(word, NamedTextColor.YELLOW));
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
        return "core.command.chat-filter-remove.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.remove.usage");
                return;
            }

            String word = args[0].toLowerCase();

            filterService.removeFilteredWord(word).thenAccept(success -> {
                if (success) {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.remove.success",
                            Component.text(word, NamedTextColor.YELLOW));
                } else {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.remove.notfound",
                            Component.text(word, NamedTextColor.YELLOW));
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
        return "core.command.chat-filter-list.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            Set<String> filteredWords = filterService.getFilteredWords();

            if (filteredWords.isEmpty()) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.list.empty");
                return;
            }

            Component message = Translations.component("core.command.chatfilter.list.header").color(NamedTextColor.GREEN);
            List<String> sortedWords = new ArrayList<>(filteredWords);
            sortedWords.sort(String::compareTo);

            for (String word : sortedWords) {
                message = message.appendNewline().append(Component.text("- ", NamedTextColor.GRAY))
                        .append(Component.text(word, NamedTextColor.YELLOW));
            }

            UtilMessage.message(player, COMMAND_PREFIX, message);
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
        return "core.command.chat-filter-reload.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (filterService instanceof DatabaseFilterService databaseFilterService) {
                databaseFilterService.loadFilteredWords();
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.chatfilter.reload.done");
            }
        }
    }
}
