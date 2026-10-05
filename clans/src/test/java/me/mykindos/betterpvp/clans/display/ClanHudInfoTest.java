package me.mykindos.betterpvp.clans.display;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.utilities.model.display.PlayerHeadProvider;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClanHudInfoTest {

    private static boolean paints(Component tree, Component head) {
        if (tree == head) {
            return true;
        }
        return tree.children().stream().anyMatch(child -> paints(child, head));
    }

    @Test
    @DisplayName("AC1: the HUD redraws when the head it paints finishes loading, with no other input changed")
    void ac1_redrawsWhenPaintedHeadLoads() {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.locale()).thenReturn(Locale.ENGLISH);

        final Gamer gamer = mock(Gamer.class);
        when(gamer.getPlayer()).thenReturn(player);
        doReturn(Optional.of(250)).when(gamer).getProperty(GamerProperty.BALANCE);

        final ClanManager clanManager = mock(ClanManager.class);
        when(clanManager.getClanByPlayer(player)).thenReturn(Optional.empty());
        final ZoneManager zoneManager = mock(ZoneManager.class);

        // Any other layout of the head is already loaded and never changes, so it cannot signal the redraw.
        final Component otherLayout = Component.text("other layout head");
        final Component placeholder = Component.text("placeholder head");
        final Component loaded = Component.text("loaded head");
        final AtomicReference<Component> painted = new AtomicReference<>(placeholder);
        final PlayerHeadProvider heads = mock(PlayerHeadProvider.class);
        lenient().when(heads.head(eq(player), anyInt(), anyInt())).thenReturn(Optional.of(otherLayout));
        when(heads.head(player, 3, 16)).thenAnswer(invocation -> Optional.of(painted.get()));

        final ClanHudInfo hud = new ClanHudInfo(clanManager, zoneManager, heads);
        final Component first = hud.render(gamer);
        assertTrue(paints(first, placeholder), "first frame paints the head that is not loaded yet");

        painted.set(loaded);
        final Component second = hud.render(gamer);
        assertNotSame(first, second, "the HUD redraws once the painted head loads");
        assertTrue(paints(second, loaded), "the redrawn frame paints the loaded head");
    }
}
