package me.mykindos.betterpvp.core.menu.dialog;

import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DialogSessions")
class DialogSessionsTest {

    private final List<CompiledDialog> shown = new ArrayList<>();
    private final List<Runnable> scheduled = new ArrayList<>();
    private DialogSessions sessions;
    private Player player;

    @BeforeEach
    void setUp() {
        sessions = new DialogSessions(new DialogSender() {
            @Override
            public void show(Player target, CompiledDialog dialog) {
                shown.add(dialog);
            }

            @Override
            public void close(Player target) {
            }
        }, (ticks, task) -> scheduled.add(task));
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private static Key regionKey(CompiledDialog dialog) {
        for (Component child : dialog.getBody().children()) {
            final ClickEvent click = child.clickEvent();
            if (click != null && click.payload() instanceof ClickEvent.Payload.Custom custom) {
                return custom.key();
            }
        }
        throw new AssertionError("No clickable region in the body");
    }

    private DialogScreen screenWithRegion(DialogClick click) {
        final DialogCanvas canvas = new DialogCanvas(200);
        canvas.text(10, 0, Component.text("Tab").font(Resources.Font.UI)).onClick(click);
        return DialogScreen.builder().name(Component.text("Test")).canvas(canvas).build();
    }

    private DialogScreen screenWithField(DialogClick click) {
        return DialogScreen.builder()
                .name(Component.text("Test"))
                .canvas(new DialogCanvas(200))
                .field(DialogField.Text.builder().key("name").label(Component.text("Name")).build())
                .button(DialogButton.builder().label(Component.text("Save")).click(click).build())
                .build();
    }

    @Test
    @DisplayName("AC5: a region's callback runs for the player who clicked it")
    void ac5_regionCallbackRunsForClicker() {
        final AtomicReference<Player> clicked = new AtomicReference<>();
        sessions.open(player, screenWithRegion((who, inputs) -> clicked.set(who)));

        assertTrue(sessions.handle(player, regionKey(shown.getLast()), DialogInputs.EMPTY));
        assertSame(player, clicked.get());
    }

    @Test
    @DisplayName("AC5: a native button's callback gets the screen's input values")
    void ac5_buttonCallbackGetsInputs() {
        final AtomicReference<String> name = new AtomicReference<>();
        sessions.open(player, screenWithField((who, inputs) -> name.set(inputs.text("name"))));

        final Key action = shown.getLast().getButtons().getFirst().getAction();
        assertTrue(sessions.handle(player, action, new DialogInputs(Map.of("name", "Bob"))));
        assertEquals("Bob", name.get());
    }

    @Test
    @DisplayName("AC6: clicks from a replaced or closed screen do nothing")
    void ac6_staleClicksIgnored() {
        final AtomicReference<String> ran = new AtomicReference<>();
        sessions.open(player, screenWithRegion((who, inputs) -> ran.set("first")));
        final Key first = regionKey(shown.getLast());
        sessions.open(player, screenWithRegion((who, inputs) -> ran.set("second")));
        final Key second = regionKey(shown.getLast());

        assertFalse(sessions.handle(player, first, DialogInputs.EMPTY));
        assertNull(ran.get());

        sessions.close(player);
        assertFalse(sessions.handle(player, second, DialogInputs.EMPTY));
        assertNull(ran.get());
    }

    @Test
    @DisplayName("AC7: re-rendering sends typed values back as initial values")
    void ac7_rerenderKeepsInputs() {
        sessions.open(player, screenWithField((who, inputs) -> sessions.rerender(who)));

        final Key action = shown.getLast().getButtons().getFirst().getAction();
        sessions.handle(player, action, new DialogInputs(Map.of("name", "Bob")));

        assertEquals(2, shown.size());
        final DialogField.Text field = (DialogField.Text) shown.getLast().getFields().getFirst();
        assertEquals("Bob", field.getInitial());
    }

    @Test
    @DisplayName("AC17: a click on an element with pressed art shows the pressed art first, then runs and settles")
    void ac17_pressedArtShowsBeforeClickRuns() {
        final AtomicReference<String> ran = new AtomicReference<>();
        final DialogCanvas canvas = new DialogCanvas(200);
        canvas.text(10, 0, Component.text("Up").font(Resources.Font.UI))
                .pressed(Component.text("Down").font(Resources.Font.UI))
                .onClick((who, inputs) -> ran.set("clicked"));
        sessions.open(player, DialogScreen.builder().name(Component.text("Test")).canvas(canvas).build());

        sessions.handle(player, regionKey(shown.getLast()), DialogInputs.EMPTY);
        assertTrue(bodyHas(shown.getLast(), "Down"));
        assertFalse(bodyHas(shown.getLast(), "Up"));
        assertNull(ran.get());

        scheduled.forEach(Runnable::run);
        assertEquals("clicked", ran.get());
        assertTrue(bodyHas(shown.getLast(), "Up"));
    }

    private static boolean bodyHas(CompiledDialog dialog, String content) {
        return dialog.getBody().children().stream()
                .anyMatch(child -> child instanceof TextComponent text && text.content().equals(content));
    }

    @Test
    @DisplayName("AC8: a player's session is removed when they quit")
    void ac8_quitRemovesSession() {
        sessions.open(player, screenWithRegion((who, inputs) -> { }));
        assertTrue(sessions.isOpen(player.getUniqueId()));

        sessions.onQuit(new PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED));
        assertFalse(sessions.isOpen(player.getUniqueId()));
    }
}
