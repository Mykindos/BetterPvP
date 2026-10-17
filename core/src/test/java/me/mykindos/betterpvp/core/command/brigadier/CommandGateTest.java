package me.mykindos.betterpvp.core.command.brigadier;

import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.RootCommand;
import me.mykindos.betterpvp.core.command.listener.CommandListener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.config;
import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.rootWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("Which commands a player may send")
class CommandGateTest {

    private final CommandFixtures fixtures = new CommandFixtures();
    private final BrigadierCommandManager brigadierCommands = new BrigadierCommandManager();
    private CommandListener listener;
    private Player player;

    @BeforeEach
    void setUp() {
        final RootCommand root = rootWith(fixtures.clientManager, config("root.requiredRank=PLAYER"));
        brigadierCommands.addObject("root", root);
        listener = new CommandListener(fixtures.clientManager, mock(CommandManager.class), brigadierCommands);
        player = fixtures.player("Alice", Rank.PLAYER, false);
    }

    @Test
    @DisplayName("AC12 a player who isn't an admin or op can run a Brigadier command they have the rank for")
    void ac12_brigadierCommandNotCancelled() {
        final PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/root", new HashSet<>());

        listener.onPlayerCommandPreProcess(event);

        assertFalse(event.isCancelled());
    }

    @Test
    @DisplayName("AC13 a player who isn't an admin or op can't run or see a command no BetterPvP framework registered")
    void ac13_foreignCommandBlockedAndHidden() {
        final PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/plugins", new HashSet<>());
        final List<String> sent = new ArrayList<>(List.of("plugins", "root"));

        listener.onPlayerCommandPreProcess(event);
        listener.onCommandListSent(new PlayerCommandSendEvent(player, sent));

        assertTrue(event.isCancelled());
        assertFalse(sent.contains("plugins"));
        assertTrue(sent.contains("root"));
    }
}
