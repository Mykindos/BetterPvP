package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.BankCommand;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.ConditionalCommand;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.DepositCommand;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.InfoCommand;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.PlayerOnlyCommand;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.RootCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.config;
import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.dispatcher;
import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.message;
import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.plain;
import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.rootWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Brigadier command framework")
class BrigadierCommandTest {

    private final CommandFixtures fixtures = new CommandFixtures();
    private EnglishCommandMessages messages;

    @BeforeEach
    void openMessages() {
        messages = EnglishCommandMessages.open();
    }

    @AfterEach
    void closeMessages() {
        messages.close();
    }

    private boolean canUse(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String... path) {
        var node = dispatcher.getRoot().getChild(path[0]);
        for (int i = 1; i < path.length; i++) {
            node = node.getChild(path[i]);
        }
        return node.canUse(source);
    }

    @Test
    @DisplayName("AC2 a subcommand's alias is a sibling literal that runs the same command")
    void ac2_subcommandAliasRunsSameCommand() throws CommandSyntaxException {
        final InfoCommand info = new InfoCommand(fixtures.clientManager);
        final RootCommand root = rootWith(fixtures.clientManager,
                config("root.requiredRank=PLAYER", "root.info.requiredRank=PLAYER"), info);
        final CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(root);
        final CommandSourceStack source = fixtures.source(fixtures.player("Alice", Rank.PLAYER, false));

        dispatcher.execute("root info Bob", source);
        dispatcher.execute("root i Bob", source);

        assertEquals(2, info.runs.get());
    }

    @Test
    @DisplayName("#2401 AC1 a subcommand of a subcommand is reachable through every alias")
    void ac1_nestedSubcommandReachableThroughAliases() throws CommandSyntaxException {
        final DepositCommand deposit = new DepositCommand(fixtures.clientManager);
        final BankCommand bank = new BankCommand(fixtures.clientManager);
        deposit.setParent(bank);
        bank.getChildren().add(deposit);
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=PLAYER",
                "root.bank.requiredRank=PLAYER", "root.bank.deposit.requiredRank=PLAYER"), bank);
        final CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(root);
        final CommandSourceStack source = fixtures.source(fixtures.player("Alice", Rank.PLAYER, false));

        dispatcher.execute("root bank deposit 5", source);
        dispatcher.execute("root bank d 5", source);
        dispatcher.execute("root b deposit 5", source);
        dispatcher.execute("root b d 5", source);

        assertEquals(4, deposit.runs.get());
    }

    @Test
    @DisplayName("AC3 rank and enabled are read per command path and defaults are written back")
    void ac3_configReadPerPathWithDefaultsWritten() {
        final ExtendedYamlConfiguration config = config("root.requiredRank=PLAYER");
        final InfoCommand info = new InfoCommand(fixtures.clientManager);
        final RootCommand root = rootWith(fixtures.clientManager, config, info);

        assertEquals(Rank.PLAYER, root.getRequiredRank());
        assertEquals(Rank.ADMIN, info.getRequiredRank());
        assertEquals("ADMIN", config.getString("root.info.requiredRank"));
        assertTrue(config.getBoolean("root.enabled"));
        assertTrue(config.isSet("root.info.enabled"));
    }

    @Test
    @DisplayName("AC4 an unknown rank falls back to ADMIN")
    void ac4_unknownRankFallsBackToAdmin() {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=NOT_A_RANK"));

        assertEquals(Rank.ADMIN, root.getRequiredRank());
    }

    @Test
    @DisplayName("AC6 a disabled command can't be run or seen by anyone")
    void ac6_disabledCommandHiddenFromEveryone() {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=PLAYER", "root.enabled=false"));
        final CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(root);

        assertFalse(canUse(dispatcher, fixtures.source(fixtures.player("Admin", Rank.DEVELOPER, true)), "root"));
        assertFalse(canUse(dispatcher, fixtures.source(fixtures.console()), "root"));
    }

    @Test
    @DisplayName("AC7 a player below a command's rank can't run or see it, checked per subcommand")
    void ac7_rankCheckedPerSubcommand() {
        final RootCommand root = rootWith(fixtures.clientManager,
                config("root.requiredRank=PLAYER", "root.info.requiredRank=ADMIN"), new InfoCommand(fixtures.clientManager));
        final CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(root);
        final CommandSourceStack player = fixtures.source(fixtures.player("Alice", Rank.PLAYER, false));
        final CommandSourceStack admin = fixtures.source(fixtures.player("Admin", Rank.ADMIN, false));

        assertTrue(canUse(dispatcher, player, "root"));
        assertFalse(canUse(dispatcher, player, "root", "info"));
        assertTrue(canUse(dispatcher, admin, "root", "info"));
    }

    @Test
    @DisplayName("AC8 an op can run every enabled command whatever their rank")
    void ac8_opBypassesRank() {
        final RootCommand root = rootWith(fixtures.clientManager,
                config("root.requiredRank=ADMIN", "root.info.requiredRank=ADMIN"), new InfoCommand(fixtures.clientManager));
        final CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(root);
        final CommandSourceStack op = fixtures.source(fixtures.player("Op", Rank.PLAYER, true));

        assertTrue(canUse(dispatcher, op, "root"));
        assertTrue(canUse(dispatcher, op, "root", "info"));
    }

    @Test
    @DisplayName("AC9 console can run an enabled command")
    void ac9_consoleCanRunEnabledCommand() throws CommandSyntaxException {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=ADMIN"));
        final CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(root);

        dispatcher.execute("root", fixtures.source(fixtures.console()));

        assertEquals(1, root.runs.get());
    }

    @Test
    @DisplayName("AC9 a player-only command run from console says it needs a player")
    void ac9_playerOnlyCommandRepliesToConsole() {
        final RootCommand root = rootWith(fixtures.clientManager,
                config("root.requiredRank=PLAYER", "root.me.requiredRank=PLAYER"), new PlayerOnlyCommand(fixtures.clientManager));
        final CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(root);

        final CommandSyntaxException exception = assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("root me", fixtures.source(fixtures.console())));

        assertEquals("Only players can run this command", message(exception));
    }

    @Test
    @DisplayName("AC10 a command's own requirement hides its literal")
    void ac10_ownRequirementHidesLiteral() {
        final ConditionalCommand create = new ConditionalCommand(fixtures.clientManager);
        final RootCommand root = rootWith(fixtures.clientManager,
                config("root.requiredRank=PLAYER", "root.create.requiredRank=PLAYER"), create);
        final CommandSourceStack player = fixtures.source(fixtures.player("Alice", Rank.PLAYER, false));

        assertFalse(canUse(dispatcher(root), player, "root", "create"));
        create.allowed = true;
        assertTrue(canUse(dispatcher(root), player, "root", "create"));
    }

    @Test
    @DisplayName("AC11 argument suggestions appear only when the sender meets the argument's requirement")
    void ac11_argumentSuggestionsFollowRequirement() {
        final AtomicBoolean allowed = new AtomicBoolean(false);
        final ArgumentType<String> suggesting = new ArgumentType<>() {
            @Override
            public String parse(StringReader reader) {
                return reader.readUnquotedString();
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return builder.suggest("alpha").buildFuture();
            }
        };
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(BrigadierNodes.literal("x")
                .then(BrigadierNodes.argument("value", suggesting, source -> allowed.get()).executes(context -> 1)));
        final CommandSourceStack source = fixtures.source(fixtures.player("Alice", Rank.PLAYER, false));

        assertTrue(dispatcher.getCompletionSuggestions(dispatcher.parse("x ", source)).join().isEmpty());
        allowed.set(true);
        assertEquals("alpha", dispatcher.getCompletionSuggestions(dispatcher.parse("x ", source)).join().getList().getFirst().getText());
    }

    @Test
    @DisplayName("AC14 selector use follows the minecraft.command.selector permission")
    void ac14_selectorFollowsPermission() {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=PLAYER"));
        final Player allowed = fixtures.player("Mod", Rank.MODERATOR, false);
        final Player denied = fixtures.player("Alice", Rank.PLAYER, false);
        when(allowed.hasPermission("minecraft.command.selector")).thenReturn(true);

        assertTrue(root.hasSelector(fixtures.source(allowed)));
        assertFalse(root.hasSelector(fixtures.source(denied)));
    }

    @Test
    @DisplayName("AC21 a syntax error is sent in red as plain text")
    void ac21_syntaxErrorSentAsPlainRedText() {
        final CommandSender sender = mock(CommandSender.class);
        final CommandSyntaxException exception = new SimpleCommandExceptionType(new LiteralMessage("<bold>boom</bold>")).create();

        UtilMessage.sendCommandSyntaxException(sender, exception);

        final ArgumentCaptor<Component> sent = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        assertEquals("<bold>boom</bold>", plain(sent.getValue()));
        assertEquals(NamedTextColor.RED, sent.getValue().color());
    }

    @Test
    @DisplayName("AC27 a framework error reaches the sender as a translatable component, not flattened English")
    void ac27_frameworkErrorStaysTranslatable() {
        final CommandSender sender = mock(CommandSender.class);

        UtilMessage.sendCommandSyntaxException(sender, ArgumentException.PLAYER_ONLY.create());

        final ArgumentCaptor<Component> sent = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        assertTrue(containsKey(sent.getValue(), "core.command.error.player_only"), sent.getValue().toString());
        assertEquals("Only players can run this command", plain(sent.getValue()));
        assertEquals(NamedTextColor.RED, sent.getValue().color());
    }

    private static boolean containsKey(Component component, String key) {
        if (component instanceof TranslatableComponent translatable && translatable.key().equals(key)) {
            return true;
        }
        return component.children().stream().anyMatch(child -> containsKey(child, key));
    }

    @Test
    @DisplayName("AC22 an unknown offline player is reported and the lookup comes back empty")
    void ac22_unknownOfflinePlayerReported() {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=PLAYER"));
        final CommandSender sender = mock(CommandSender.class);
        fixtures.noOfflineClient("Bob");

        assertEquals(Optional.empty(), root.offlineClient("Bob", sender).join());

        final ArgumentCaptor<Component> sent = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        assertEquals("Unknown Player Bob", plain(sent.getValue()));
    }

    @Test
    @DisplayName("#2401 AC3 a failed offline lookup tells the sender with a translated error")
    void ac3_failedOfflineLookupReported() {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=PLAYER"));
        final CommandSender sender = mock(CommandSender.class);
        fixtures.offlineLookupFails("Bob");

        assertEquals(Optional.empty(), root.offlineClient("Bob", sender).join());

        final ArgumentCaptor<Component> sent = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        assertTrue(containsKey(sent.getValue(), "core.command.error.offline_lookup_failed"), sent.getValue().toString());
        assertEquals("Could not look up Bob, try again", plain(sent.getValue()));
        assertEquals(NamedTextColor.RED, sent.getValue().color());
    }

    @Test
    @DisplayName("AC23 usages list one line per usable path, prefixed by the parent's usage")
    void ac23_usagesOneLinePerPath() {
        final ConditionalCommand create = new ConditionalCommand(fixtures.clientManager);
        create.allowed = true;
        final PlayerOnlyCommand me = new PlayerOnlyCommand(fixtures.clientManager);
        final RootCommand root = rootWith(fixtures.clientManager,
                config("root.requiredRank=PLAYER", "root.create.requiredRank=PLAYER", "root.me.requiredRank=PLAYER"), create, me);
        final CommandSourceStack source = fixtures.source(fixtures.player("Alice", Rank.PLAYER, false));

        assertEquals("root\nroot create\nroot me", root.getUsages(source, null));
        assertEquals("root me", me.getUsages(source, "root"));
    }

    @Test
    @DisplayName("AC24 the requirement summary shows whether the sender can run the command, enabled, rank and qualification")
    void ac24_requirementSummary() throws CommandSyntaxException {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=ADMIN"));

        final String admin = plain(root.getRequirementComponent(fixtures.context(fixtures.source(fixtures.player("Admin", Rank.ADMIN, false)))));
        final String player = plain(root.getRequirementComponent(fixtures.context(fixtures.source(fixtures.player("Alice", Rank.PLAYER, false)))));

        assertTrue(admin.contains("You can run this command"), admin);
        assertTrue(admin.contains("Enabled: true"), admin);
        assertTrue(admin.contains("Rank: ADMIN"), admin);
        assertTrue(admin.contains("You Qualify: true"), admin);
        assertTrue(player.contains("You cannot run this command"), player);
        assertTrue(player.contains("You Qualify: false"), player);
    }

    @Test
    @DisplayName("CLANS-53 AC1 BrigadierCommand is the only command type, with no IBrigadierCommand interface")
    void ac1_interfaceFolded() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName(BrigadierCommand.class.getPackageName() + ".IBrigadierCommand"));
    }
}
