package me.mykindos.betterpvp.core.scene.command;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.mob.StaffTestMob;
import me.mykindos.betterpvp.core.scene.mob.StaffTestMobFactory;
import me.mykindos.betterpvp.core.world.construction.ComponentKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestMobCommandTest {

    private final StaffTestMobFactory factory = mock(StaffTestMobFactory.class);
    private final SceneObjectRegistry registry = mock(SceneObjectRegistry.class);
    private final Player player = mock(Player.class);
    private final World world = mock(World.class);
    private final Block target = mock(Block.class);

    @BeforeEach
    void setUp() {
        when(target.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(player.getTargetBlockExact(anyInt())).thenReturn(target);
    }

    @Test
    void ac1_spawnPutsATestMobOnTheBlockThePlayerLooksAt() {
        new TestMobSpawnCommand(factory).execute(player, mock(Client.class));

        verify(factory).spawn(new Location(world, 10.5, 65, 20.5));
        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.command.testmob.spawn.done"));
    }

    @Test
    void ac1_lookingAtNothingSpawnsNothing() {
        when(player.getTargetBlockExact(anyInt())).thenReturn(null);

        new TestMobSpawnCommand(factory).execute(player, mock(Client.class));

        verify(factory, never()).spawn(any());
        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.command.testmob.spawn.no_block"));
    }

    @Test
    void ac1_clearRemovesEveryTestMob() {
        final StaffTestMob first = mock(StaffTestMob.class);
        final StaffTestMob second = mock(StaffTestMob.class);
        when(registry.getObjects(StaffTestMob.class)).thenReturn(List.of(first, second));

        new TestMobClearCommand(registry).execute(player, mock(Client.class));

        verify(first).remove();
        verify(second).remove();
        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.command.testmob.clear.done"));
    }

    @Test
    void ac1_clearWithNoTestMobsSaysSo() {
        when(registry.getObjects(StaffTestMob.class)).thenReturn(List.of());

        new TestMobClearCommand(registry).execute(player, mock(Client.class));

        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.command.testmob.clear.none"));
    }

    private @NotNull Component lastMessage() {
        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(captor.capture());
        return captor.getValue();
    }
}
