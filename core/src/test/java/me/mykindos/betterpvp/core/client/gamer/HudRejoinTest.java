package me.mykindos.betterpvp.core.client.gamer;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.repository.GamerBossBarListener;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarController;
import me.mykindos.betterpvp.core.quest.QuestManager;
import me.mykindos.betterpvp.core.quest.QuestRegistry;
import me.mykindos.betterpvp.core.quest.QuestTrackerHud;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarColor;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarData;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarOverlay;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Boss-bar HUD on a fast rejoin")
class HudRejoinTest {

    private final UUID uuid = UUID.randomUUID();
    private final Gamer gamer = new Gamer(1L, uuid.toString());
    private final Player player = mock(Player.class);
    private final Client client = mock(Client.class);
    private final ClientManager clientManager = mock(ClientManager.class, Answers.RETURNS_DEEP_STUBS);
    private final Core core = mock(Core.class);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Rejoiner");
        when(player.isOnline()).thenReturn(true);
        when(client.getGamer()).thenReturn(gamer);
        when(clientManager.search().online(player)).thenReturn(client);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(player);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    @DisplayName("AC1: joining twice with the same gamer registers exactly one HUD overlay")
    void ac1_joiningTwiceRegistersOneHudOverlay() throws ReflectiveOperationException {
        final Constructor<SidebarController> constructor = SidebarController.class.getDeclaredConstructor(ClientManager.class, Core.class);
        constructor.setAccessible(true);
        final SidebarController sidebar = constructor.newInstance(clientManager, core);

        sidebar.onJoin(new PlayerJoinEvent(player, Component.empty()));
        sidebar.onJoin(new PlayerJoinEvent(player, Component.empty()));

        assertEquals(1, gamer.getBossBarOverlay().size());
    }

    @Test
    @DisplayName("AC2: quitting drops the player from the overlay and queue bars and clears their entries")
    void ac2_quitDropsViewerAndClearsOverlayAndQueue() {
        gamer.getBossBarOverlay().add(new DisplayObject<>(ignored -> Component.text("hud")));
        gamer.getBossBarQueue().add(1, BossBarColor.BLUE, new DisplayObject<>(ignored -> new BossBarData(Component.text("bar"), 1f)));
        gamer.getBossBarOverlay().show(gamer);
        gamer.getBossBarQueue().show(gamer);

        final ArgumentCaptor<BossBar> shown = ArgumentCaptor.forClass(BossBar.class);
        verify(player, atLeastOnce()).showBossBar(shown.capture());
        assertEquals(2, shown.getAllValues().stream().distinct().count());

        new GamerBossBarListener(clientManager).onQuit(new PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED));

        for (BossBar bar : shown.getAllValues()) {
            verify(player, atLeastOnce()).hideBossBar(bar);
        }
        assertFalse(gamer.getBossBarOverlay().hasOverlays());
        assertFalse(gamer.getBossBarQueue().hasElementsQueued());
    }

    @Test
    @DisplayName("AC3: a fast rejoin leaves exactly one quest tracker overlay")
    void ac3_fastRejoinKeepsOneQuestTrackerOverlay() throws ReflectiveOperationException {
        final QuestTrackerHud tracker = new QuestTrackerHud(mock(QuestManager.class), mock(QuestRegistry.class));
        final GamerBossBarListener quitListener = new GamerBossBarListener(clientManager);

        tracker.onJoin(new ClientJoinEvent(client, player));
        final DisplayObject<Component> firstOverlay = overlays().getFirst();
        quitListener.onQuit(new PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED));
        tracker.onJoin(new ClientJoinEvent(client, player));

        assertEquals(1, gamer.getBossBarOverlay().size());
        assertSame(firstOverlay, overlays().getFirst());
    }

    @SuppressWarnings("unchecked")
    private List<DisplayObject<Component>> overlays() throws ReflectiveOperationException {
        final Field field = BossBarOverlay.class.getDeclaredField("overlays");
        field.setAccessible(true);
        return (List<DisplayObject<Component>>) field.get(gamer.getBossBarOverlay());
    }
}
