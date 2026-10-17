package me.mykindos.betterpvp.clans.commands.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.ClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.member.ClanMemberArgument;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Clan commands run from console")
class ConsoleClanCommandsTest {

    private final ClanManager clanManager = mock(ClanManager.class);
    private final CommandSourceStack console = mock(CommandSourceStack.class);
    private MockedStatic<MessageComponentSerializer> serializerStatic;

    @BeforeEach
    void setUp() {
        final ConsoleCommandSender sender = mock(ConsoleCommandSender.class);
        when(sender.getName()).thenReturn("CONSOLE");
        when(console.getSender()).thenReturn(sender);

        final MessageComponentSerializer serializer = mock(MessageComponentSerializer.class);
        when(serializer.serialize(any(Component.class))).thenReturn(new LiteralMessage("message"));
        serializerStatic = Mockito.mockStatic(MessageComponentSerializer.class);
        serializerStatic.when(MessageComponentSerializer::message).thenReturn(serializer);
    }

    @AfterEach
    void tearDown() {
        serializerStatic.close();
    }

    @SuppressWarnings("unchecked")
    private CommandContext<CommandSourceStack> context() {
        final CommandContext<CommandSourceStack> context = mock(CommandContext.class);
        when(context.getSource()).thenReturn(console);
        return context;
    }

    private static void assertPlayerOnly(CommandSyntaxException exception) {
        assertSame(ArgumentException.PLAYER_ONLY, exception.getType());
    }

    @Test
    @DisplayName("AC9 console can name any clan")
    void ac9_consoleNamesAnyClan() throws CommandSyntaxException {
        final Clan clan = mock(Clan.class);
        when(clanManager.getClanByName("Red")).thenReturn(Optional.of(clan));
        final ClanArgument argument = new ClanArgument(clanManager) {
        };

        assertSame(clan, argument.convert("Red", console));
    }

    @Test
    @DisplayName("AC9 a clan member argument from console is a syntax error, and offers nothing")
    void ac9_clanMemberFromConsoleIsSyntaxError() {
        final ClanMemberArgument argument = new ClanMemberArgument(clanManager);

        assertPlayerOnly(assertThrows(CommandSyntaxException.class, () -> argument.convert("Bob", console)));
        assertTrue(argument.listSuggestions(context(), new SuggestionsBuilder("", 0)).join().isEmpty());
    }

    @Test
    @DisplayName("AC9 a clan subcommand's requirement summary from console is a syntax error")
    void ac9_clanRequirementSummaryFromConsoleIsSyntaxError() {
        final BrigadierClanSubCommand command = new BrigadierClanSubCommand(mock(ClientManager.class), clanManager) {
            @Override
            public String getName() {
                return "test";
            }

            @Override
            public String getDescription() {
                return "Test";
            }

            @Override
            public LiteralArgumentBuilder<CommandSourceStack> define() {
                return IBrigadierCommand.literal(getName());
            }

            @Override
            protected ClanMember.MemberRank requiredMemberRank() {
                return ClanMember.MemberRank.RECRUIT;
            }
        };
        command.setConfig(new ExtendedYamlConfiguration());

        assertPlayerOnly(assertThrows(CommandSyntaxException.class, () -> command.getRequirementComponent(context())));
    }
}
