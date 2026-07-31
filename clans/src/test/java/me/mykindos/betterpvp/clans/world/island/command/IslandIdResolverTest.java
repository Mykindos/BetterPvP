package me.mykindos.betterpvp.clans.world.island.command;

import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IslandIdResolverTest {

    @Mock
    private Player player;

    @Mock
    private IslandInstanceManager instanceManager;

    @Test
    @DisplayName("resolve is empty and messages the player when no instance matches the prefix")
    void zeroMatchesMessagesPlayer() {
        when(instanceManager.matchIdPrefix("ffffffff")).thenReturn(List.of());

        final Optional<IslandInstance> resolved = IslandIdResolver.resolve(player, instanceManager, "ffffffff");

        assertTrue(resolved.isEmpty());
        verify(player).sendMessage(org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
    }

    @Test
    @DisplayName("resolve returns the single instance when the prefix is unique")
    void uniqueMatchResolves() {
        final IslandInstance instance = mock(IslandInstance.class);
        when(instanceManager.matchIdPrefix("1111")).thenReturn(List.of(instance));

        final Optional<IslandInstance> resolved = IslandIdResolver.resolve(player, instanceManager, "1111");

        assertTrue(resolved.isPresent());
    }

    @Test
    @DisplayName("resolve is empty and messages the player when the prefix is ambiguous")
    void ambiguousMatchMessagesPlayer() {
        final IslandInstance first = mock(IslandInstance.class);
        final IslandInstance second = mock(IslandInstance.class);
        when(first.getId()).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(second.getId()).thenReturn(UUID.fromString("11112222-2222-2222-2222-222222222222"));
        when(instanceManager.matchIdPrefix("1111")).thenReturn(List.of(first, second));

        final Optional<IslandInstance> resolved = IslandIdResolver.resolve(player, instanceManager, "1111");

        assertTrue(resolved.isEmpty());
        verify(player).sendMessage(org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
    }
}
