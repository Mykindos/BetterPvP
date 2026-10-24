package me.mykindos.betterpvp.core.command.brigadier.arguments.types;

import com.google.inject.Injector;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.EnglishCommandMessages;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDItem;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDManager;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Brigadier argument types")
class ArgumentTypesTest {

    private final ClientManager clientManager = mock(ClientManager.class);
    @SuppressWarnings("unchecked")
    private final SearchEngineBase<Client> search = mock(SearchEngineBase.class);
    private final EffectManager effectManager = mock(EffectManager.class);
    private MockedStatic<Bukkit> bukkit;
    private EnglishCommandMessages messages;
    private Player visible;
    private Player ghost;

    /**
     * Effect types look up the Core plugin while the class initialises, so a stand-in plugin is in place for that once.
     */
    @BeforeAll
    static void initialiseEffectTypes() {
        try (MockedStatic<JavaPlugin> plugins = Mockito.mockStatic(JavaPlugin.class)) {
            final Injector injector = mock(Injector.class);
            when(injector.getInstance(any(Class.class))).thenAnswer(invocation -> mock(invocation.<Class<?>>getArgument(0)));
            plugins.when(() -> JavaPlugin.getPlugin(any())).thenAnswer(invocation -> {
                final Object plugin = mock(invocation.<Class<?>>getArgument(0));
                if (plugin instanceof BPvPPlugin bpvp) {
                    when(bpvp.getInjector()).thenReturn(injector);
                }
                return plugin;
            });
            EffectTypes.getEffectTypeByName("");
        }
    }

    @BeforeEach
    void setUp() {
        when(clientManager.search()).thenReturn(search);
        messages = EnglishCommandMessages.open();
        bukkit = Mockito.mockStatic(Bukkit.class);
        visible = player("Visible", Rank.PLAYER);
        ghost = player("Ghost", Rank.PLAYER);
        when(effectManager.hasEffect(eq(ghost), any(), eq("commandVanish"))).thenReturn(true);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(visible, ghost));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        messages.close();
    }

    private Player player(String name, Rank rank) {
        final Player player = mock(Player.class);
        final UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.canSee(any(Player.class))).thenReturn(true);
        final Client client = mock(Client.class);
        when(client.hasRank(any())).thenAnswer(invocation -> rank.getId() >= invocation.<Rank>getArgument(0).getId());
        when(search.online(player)).thenReturn(client);
        bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(player);
        bukkit.when(() -> Bukkit.getPlayerExact(name)).thenReturn(player);
        return player;
    }

    private CommandSourceStack source(Player sender) {
        final CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        when(source.getExecutor()).thenReturn(sender);
        return source;
    }

    @SuppressWarnings("unchecked")
    private CommandContext<CommandSourceStack> context(CommandSourceStack source) {
        final CommandContext<CommandSourceStack> context = mock(CommandContext.class);
        when(context.getSource()).thenReturn(source);
        return context;
    }

    private static List<String> texts(CompletableFuture<Suggestions> suggestions) {
        return suggestions.join().getList().stream().map(Suggestion::getText).toList();
    }

    private static String message(CommandSyntaxException exception) {
        return exception.getRawMessage().getString();
    }

    @Test
    @DisplayName("AC15 playerName accepts 1-16 letters, digits and underscores and rejects anything else")
    void ac15_playerNameValidated() throws CommandSyntaxException {
        final PlayerNameArgumentType type = new PlayerNameArgumentType(effectManager, clientManager);

        assertEquals("Valid_Name1", type.convert("Valid_Name1"));
        assertEquals("Invalid Playername bad-name",
                message(assertThrows(CommandSyntaxException.class, () -> type.convert("bad-name"))));
        assertThrows(CommandSyntaxException.class, () -> type.convert("a".repeat(17)));
    }

    @Test
    @DisplayName("AC16 player suggestions hide command-vanished players from senders below HELPER")
    void ac16_suggestionsHideVanishedBelowHelper() {
        final PlayerNameArgumentType playerName = new PlayerNameArgumentType(effectManager, clientManager);
        final OnlinePlayerNameArgument onlinePlayer = new OnlinePlayerNameArgument(effectManager, clientManager);
        final CommandContext<CommandSourceStack> asPlayer = context(source(player("Alice", Rank.PLAYER)));
        final CommandContext<CommandSourceStack> asHelper = context(source(player("Helper", Rank.HELPER)));

        assertEquals(List.of("Visible"), texts(playerName.suggestions(asPlayer, new SuggestionsBuilder("", 0))));
        assertEquals(List.of("Ghost", "Visible"), texts(playerName.suggestions(asHelper, new SuggestionsBuilder("", 0))));
        assertEquals(List.of("Visible"), texts(onlinePlayer.listSuggestions(asPlayer, new SuggestionsBuilder("", 0))));
        assertEquals(List.of("Ghost", "Visible"), texts(onlinePlayer.listSuggestions(asHelper, new SuggestionsBuilder("", 0))));
    }

    @Test
    @DisplayName("AC17 onlinePlayer resolves an exact online name and treats offline or hidden players as unknown")
    void ac17_onlinePlayerResolves() throws CommandSyntaxException {
        final OnlinePlayerNameArgument type = new OnlinePlayerNameArgument(effectManager, clientManager);
        final CommandSourceStack asPlayer = source(player("Alice", Rank.PLAYER));
        final CommandSourceStack asHelper = source(player("Helper", Rank.HELPER));

        assertSame(visible, type.convert("Visible", asPlayer));
        assertSame(ghost, type.convert("Ghost", asHelper));
        assertEquals("Ghost does not match an online player",
                message(assertThrows(CommandSyntaxException.class, () -> type.convert("Ghost", asPlayer))));
        assertEquals("Nobody does not match an online player",
                message(assertThrows(CommandSyntaxException.class, () -> type.convert("Nobody", asPlayer))));
    }

    private CommandSourceStack console() {
        final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        when(console.getName()).thenReturn("CONSOLE");
        final CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(console);
        return source;
    }

    @Test
    @DisplayName("AC9 console names and is offered every online player, vanished or not")
    void ac9_consoleSeesEveryPlayer() throws CommandSyntaxException {
        final PlayerNameArgumentType playerName = new PlayerNameArgumentType(effectManager, clientManager);
        final OnlinePlayerNameArgument onlinePlayer = new OnlinePlayerNameArgument(effectManager, clientManager);
        final CommandSourceStack console = console();

        assertSame(ghost, onlinePlayer.convert("Ghost", console));
        assertEquals(List.of("Ghost", "Visible"), texts(onlinePlayer.listSuggestions(context(console), new SuggestionsBuilder("", 0))));
        assertEquals(List.of("Ghost", "Visible"), texts(playerName.suggestions(context(console), new SuggestionsBuilder("", 0))));
    }

    @Test
    @DisplayName("AC18 boolean accepts true/yes/1 and false/no/0 in any case, suggests true and false, and rejects the rest")
    void ac18_booleanParsing() throws CommandSyntaxException {
        final BooleanArgumentType type = new BooleanArgumentType();

        for (String yes : List.of("true", "YES", "1")) {
            assertTrue(type.convert(yes), yes);
        }
        for (String no : List.of("False", "no", "0")) {
            assertEquals(false, type.convert(no), no);
        }
        assertEquals("Invalid boolean value: maybe, Expected true or false",
                message(assertThrows(CommandSyntaxException.class, () -> type.convert("maybe"))));
        assertEquals(List.of("false", "true"), texts(type.listSuggestions(context(source(visible)), new SuggestionsBuilder("", 0))));
    }

    @Test
    @DisplayName("AC19 customEffect suggests matching effects and rejects an unknown one")
    void ac19_customEffect() throws CommandSyntaxException {
        final CustomEffectArgumentType type = new CustomEffectArgumentType();
        final String vanish = EffectTypes.VANISH.getName().replace(" ", "_");

        assertSame(EffectTypes.VANISH, type.convert(vanish));
        assertEquals("Unknown Effect nope", message(assertThrows(CommandSyntaxException.class, () -> type.convert("nope"))));
        assertTrue(texts(type.listSuggestions(context(source(visible)), new SuggestionsBuilder(vanish.substring(0, 3), 0))).contains(vanish));
    }

    @Test
    @DisplayName("AC19 customItem suggests matching item keys and rejects an unknown one")
    void ac19_customItem() throws CommandSyntaxException {
        final ItemRegistry registry = mock(ItemRegistry.class);
        final BaseItem sword = mock(BaseItem.class);
        final NamespacedKey swordKey = new NamespacedKey("betterpvp", "sword");
        when(registry.getItem(swordKey)).thenReturn(sword);
        when(registry.getItemsSorted()).thenReturn(Map.of(swordKey, sword, new NamespacedKey("betterpvp", "bow"), mock(BaseItem.class)));
        final CustomItemArgumentType type = new CustomItemArgumentType(registry);

        assertSame(sword, type.convert(swordKey));
        assertEquals("Unknown BPvPItem with name: betterpvp:nope",
                message(assertThrows(CommandSyntaxException.class, () -> type.convert(new NamespacedKey("betterpvp", "nope")))));
        assertEquals(List.of("betterpvp:sword"), texts(type.listSuggestions(context(source(visible)), new SuggestionsBuilder("betterpvp:sw", 0))));
    }

    @Test
    @DisplayName("AC19 uuidItem suggests matching UUIDs and rejects an unknown one")
    void ac19_uuidItem() throws CommandSyntaxException {
        final UUIDManager manager = mock(UUIDManager.class);
        final UUIDItem item = mock(UUIDItem.class);
        final UUID known = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final UUID unknown = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(manager.getObject(known)).thenReturn(Optional.of(item));
        when(manager.getObject(unknown)).thenReturn(Optional.empty());
        when(manager.getObjects()).thenReturn(Map.of(known.toString(), item));
        final UUIDItemArgumentType type = new UUIDItemArgumentType(manager);

        assertSame(item, type.convert(known));
        assertEquals("Unknown UUIDItem with UUID: " + unknown,
                message(assertThrows(CommandSyntaxException.class, () -> type.convert(unknown))));
        assertEquals(List.of(known.toString()), texts(type.listSuggestions(context(source(visible)), new SuggestionsBuilder("1111", 0))));
    }

    @Test
    @DisplayName("AC20 duration parses a positive number and unit or perm, rejects anything else or an overflow, and offers no suggestions")
    void ac20_durationHintOnly() throws CommandSyntaxException {
        final DurationArgumentType type = new DurationArgumentType();

        assertEquals(30 * 60 * 1000L, type.convert("30m"));
        assertEquals(2 * 24 * 60 * 60 * 1000L, type.convert("2d"));
        assertEquals(-1L, type.convert("perm"));
        assertEquals("Invalid duration: soon", message(assertThrows(CommandSyntaxException.class, () -> type.convert("soon"))));
        assertEquals("Invalid duration: 300000000y", message(assertThrows(CommandSyntaxException.class, () -> type.convert("300000000y"))));
        assertEquals("Invalid duration: 0m", message(assertThrows(CommandSyntaxException.class, () -> type.convert("0m"))));
        assertTrue(type.listSuggestions(context(source(visible)), new SuggestionsBuilder("", 0)).join().isEmpty());
        assertEquals("Duration", type.getName());
    }
}
