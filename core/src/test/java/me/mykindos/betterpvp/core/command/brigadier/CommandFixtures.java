package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mocked senders, a client lookup and small commands for exercising the Brigadier framework without a server.
 */
final class CommandFixtures {

    final ClientManager clientManager = mock(ClientManager.class);
    @SuppressWarnings("unchecked")
    final SearchEngineBase<Client> search = mock(SearchEngineBase.class);

    CommandFixtures() {
        when(clientManager.search()).thenReturn(search);
    }

    Player player(String name, Rank rank, boolean op) {
        final Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
        when(player.isOp()).thenReturn(op);

        final Client client = mock(Client.class);
        when(client.getName()).thenReturn(name);
        when(client.getRank()).thenReturn(rank);
        when(client.hasRank(any())).thenAnswer(invocation -> rank.getId() >= invocation.<Rank>getArgument(0).getId());
        when(search.online(player)).thenReturn(client);
        return player;
    }

    ConsoleCommandSender console() {
        final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        when(console.getName()).thenReturn("CONSOLE");
        return console;
    }

    CommandSourceStack source(CommandSender sender) {
        final CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        when(source.getExecutor()).thenReturn(sender instanceof Player player ? player : null);
        return source;
    }

    @SuppressWarnings("unchecked")
    CommandContext<CommandSourceStack> context(CommandSourceStack source) {
        final CommandContext<CommandSourceStack> context = mock(CommandContext.class);
        when(context.getSource()).thenReturn(source);
        return context;
    }

    void noOfflineClient(String name) {
        when(search.offline(name)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * A config holding {@code path=value} pairs, for example {@code "root.requiredRank=PLAYER"}.
     */
    static ExtendedYamlConfiguration config(String... entries) {
        final ExtendedYamlConfiguration config = new ExtendedYamlConfiguration();
        for (String entry : entries) {
            final String[] pair = entry.split("=", 2);
            config.set(pair[0], pair[1].equals("true") || pair[1].equals("false") ? Boolean.valueOf(pair[1]) : pair[1]);
        }
        return config;
    }

    static CommandDispatcher<CommandSourceStack> dispatcher(IBrigadierCommand command) {
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(command.build());
        return dispatcher;
    }

    static String plain(Component component) {
        return EnglishCommandMessages.english(component);
    }

    /**
     * {@code /root}, alias {@code /r}. Counts its runs.
     */
    static class RootCommand extends BrigadierCommand {
        final AtomicInteger runs = new AtomicInteger();

        RootCommand(ClientManager clientManager) {
            super(clientManager);
            getAliases().add("r");
        }

        @Override
        public String getName() {
            return "root";
        }

        @Override
        public String getDescription() {
            return "Root command";
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> define() {
            return IBrigadierCommand.literal(getName()).executes(context -> runs.incrementAndGet());
        }

        CompletableFuture<Optional<Client>> offlineClient(String name, CommandSender sender) {
            return getOfflineClientByName(name, sender);
        }

        boolean hasSelector(CommandSourceStack source) {
            return senderHasSelector(source);
        }
    }

    /**
     * {@code /root info <target>}, alias {@code /root i}. Counts its runs.
     */
    static class InfoCommand extends BrigadierCommand {
        final AtomicInteger runs = new AtomicInteger();

        InfoCommand(ClientManager clientManager) {
            super(clientManager);
            getAliases().add("i");
        }

        @Override
        public String getName() {
            return "info";
        }

        @Override
        public String getDescription() {
            return "Info";
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> define() {
            return IBrigadierCommand.literal(getName())
                    .then(IBrigadierCommand.argument("target", StringArgumentType.word())
                            .executes(context -> runs.incrementAndGet()));
        }
    }

    /**
     * {@code /root me}, which needs a player executor.
     */
    static class PlayerOnlyCommand extends BrigadierCommand {

        PlayerOnlyCommand(ClientManager clientManager) {
            super(clientManager);
        }

        @Override
        public String getName() {
            return "me";
        }

        @Override
        public String getDescription() {
            return "Player only";
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> define() {
            return IBrigadierCommand.literal(getName()).executes(context -> {
                getClientFromExecutor(context);
                return 1;
            });
        }
    }

    /**
     * {@code /root create}, usable only while {@link #allowed} is true.
     */
    static class ConditionalCommand extends BrigadierCommand {
        boolean allowed;

        ConditionalCommand(ClientManager clientManager) {
            super(clientManager);
        }

        @Override
        public String getName() {
            return "create";
        }

        @Override
        public String getDescription() {
            return "Conditional";
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> define() {
            return IBrigadierCommand.literal(getName()).executes(context -> 1);
        }

        @Override
        public boolean requirement(CommandSourceStack source) {
            return super.requirement(source) && allowed;
        }
    }

    static RootCommand rootWith(ClientManager clientManager, @Nullable ExtendedYamlConfiguration config, BrigadierCommand... children) {
        final RootCommand root = new RootCommand(clientManager);
        for (BrigadierCommand child : children) {
            child.setParent(root);
            root.getChildren().add(child);
        }
        if (config != null) {
            root.setConfig(config);
        }
        return root;
    }

    static String message(CommandSyntaxException exception) {
        return exception.getRawMessage().getString();
    }
}
