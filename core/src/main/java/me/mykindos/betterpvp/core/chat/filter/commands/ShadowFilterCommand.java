package me.mykindos.betterpvp.core.chat.filter.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.filter.ShadowChatFilterManager;
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
public class ShadowFilterCommand extends Command {

    private final ShadowChatFilterManager filterManager;

    @Inject
    public ShadowFilterCommand(ShadowChatFilterManager filterManager) {
        this.filterManager = filterManager;
    }

    @Override
    public String getName() {
        return "shadowfilter";
    }

    @Override
    public String getDescription() {
        return "Manage shadow chat filter words";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Command", UtilMessage.deserialize("<gray>Usage: /filter <add | remove | list | reload></gray>"));
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }


    @Singleton
    @SubCommand(ShadowFilterCommand.class)
    public static class ShadowFilterAddCommand extends Command {
        private final ShadowChatFilterManager filterManager;

        @Inject
        public ShadowFilterAddCommand(ShadowChatFilterManager filterManager) {
            this.filterManager = filterManager;
        }

        @Override
        public String getName() {
            return "add";
        }

        @Override
        public String getDescription() {
            return "Add a word to the shadow filter list";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "Filter", UtilMessage.deserialize("<green>Usage: /filter add <word></green>"));
                return;
            }

            String word = String.join(" ", args).toLowerCase();

            filterManager.addFilteredWord(word, client).thenAccept(success -> {
                if (success) {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("Added <yellow>%s</yellow> to the shadow filter list", word));
                } else {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("<yellow>%s</yellow> is already in the shadow filter list", word));
                }
            });
        }
    }

    @Singleton
    @SubCommand(ShadowFilterCommand.class)
    public static class ShadowFilterRemoveCommand extends Command {
        private final ShadowChatFilterManager filterManager;

        @Inject
        public ShadowFilterRemoveCommand(ShadowChatFilterManager filterManager) {
            this.filterManager = filterManager;
        }

        @Override
        public String getName() {
            return "remove";
        }

        @Override
        public String getDescription() {
            return "Remove a word from the shadow filter list";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "Filter", UtilMessage.deserialize("<green>Usage: /filter remove <word></green>"));
                return;
            }

            String word = args[0].toLowerCase();

            filterManager.removeFilteredWord(word).thenAccept(success -> {
                if (success) {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("Removed <yellow>%s</yellow> from the shadow filter list", word));
                } else {
                    UtilMessage.message(player, "Filter", UtilMessage.deserialize("<yellow>%s</yellow> is not in the shadow filter list", word));
                }
            });
        }
    }

    @Singleton
    @SubCommand(ShadowFilterCommand.class)
    public static class ShadowFilterListCommand extends Command {
        private final ShadowChatFilterManager filterManager;

        @Inject
        public ShadowFilterListCommand(ShadowChatFilterManager filterManager) {
            this.filterManager = filterManager;
        }

        @Override
        public String getName() {
            return "list";
        }

        @Override
        public String getDescription() {
            return "List all shadow filtered words";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            Set<String> filteredWords = filterManager.getFilteredWords();

            if (filteredWords.isEmpty()) {
                UtilMessage.message(player, "Filter", "There are no words in the shadow filter list");
                return;
            }

            Component message = Component.text("Shadow Filtered Words:", NamedTextColor.GREEN);
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
    @SubCommand(ShadowFilterCommand.class)
    public static class ShadowFilterReloadCommand extends Command {
        private final ShadowChatFilterManager filterManager;

        @Inject
        public ShadowFilterReloadCommand(ShadowChatFilterManager filterManager) {
            this.filterManager = filterManager;
        }

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the shadow filter list from the database";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            filterManager.loadFilteredWords();
            UtilMessage.message(player, "Filter", "Reloaded shadow filter list from database");
        }
    }
}
