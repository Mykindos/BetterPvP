package me.mykindos.betterpvp.core.menu.dialog.screen;

import me.mykindos.betterpvp.core.menu.dialog.CompiledDialog;
import me.mykindos.betterpvp.core.menu.dialog.DialogInputs;
import me.mykindos.betterpvp.core.menu.dialog.DialogSender;
import me.mykindos.betterpvp.core.menu.dialog.DialogSessions;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GuiScreens")
class GuiScreensTest {

    private final List<CompiledDialog> shown = new ArrayList<>();
    private final List<Runnable> scheduled = new ArrayList<>();
    private GuiRegistry registry;
    private GuiScreens screens;
    private DialogSessions sessions;
    private Player player;

    @BeforeEach
    void setUp() throws IOException {
        registry = new GuiRegistry();
        registry.elementType("test", "meter", (node, context) ->
                context.art(ScreenAssets.box("meter_fill", 80, 12), 0, 0));
        sessions = new DialogSessions(new DialogSender() {
            @Override
            public void show(Player target, CompiledDialog dialog) {
                shown.add(dialog);
            }

            @Override
            public void close(Player target) {
            }
        }, (ticks, task) -> scheduled.add(task));
        screens = new GuiScreens(registry, sessions);
        screens.load("golden", Map.of("gui/shop.json", ScreenAssetsTest.resource("/gui-golden/shop.json")));
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.locale()).thenReturn(Locale.ENGLISH);
    }

    /** The click key of the first body text whose content is {@code label}. */
    private Key click(String label) {
        for (Component child : shown.getLast().getBody().children()) {
            if (child instanceof TextComponent text && text.content().equals(label)
                    && child.clickEvent() != null && child.clickEvent().payload() instanceof ClickEvent.Payload.Custom custom) {
                return custom.key();
            }
        }
        throw new AssertionError("No clickable " + label);
    }

    private boolean bodyShows(String content) {
        return shown.getLast().getBody().children().stream()
                .anyMatch(child -> child instanceof TextComponent text && text.content().equals(content));
    }

    @Test
    @DisplayName("every screen shipped in core parses and validates")
    void shippedScreensValidate() {
        final GuiScreens core = new GuiScreens(new GuiRegistry(), sessions);
        core.load("core", GuiScreens.class);

        assertTrue(core.definition("core:dialog_test") != null);
        assertEquals(List.of(), core.validate());
    }

    @Test
    @DisplayName("a valid screen with its element type registered has no problems")
    void validScreen() {
        assertEquals(List.of(), screens.validate());
    }

    @Test
    @DisplayName("an unregistered element type and an unknown action are reported by name")
    void problemsNamed() throws IOException {
        final GuiScreens bare = new GuiScreens(new GuiRegistry(), new DialogSessions(null, null));
        bare.load("golden", Map.of("gui/shop.json",
                ScreenAssetsTest.resource("/gui-golden/shop.json").replace("\"actions\": [\"buy\"],", "")));

        final List<String> problems = bare.validate();
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("test:meter")), problems.toString());
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("action buy")), problems.toString());
    }

    @Test
    @DisplayName("a screen built in code that needs undeclared art is reported with the asset")
    void undeclaredArt() {
        final ScreenDefinition screen = ScreenDefinition.builder().namespace("golden").id("coded")
                .canvasWidth(300).canvasHeight(100)
                .element(Node.Box.builder().style("primary").width(50).height(20).build())
                .build();

        assertTrue(screens.validate(screen).stream().anyMatch(problem -> problem.contains("box:primary:50x20")));
    }

    @Test
    @DisplayName("a backdrop on a canvas taller than the backdrop limit is reported")
    void tallBackdrop() {
        final ScreenDefinition screen = ScreenDefinition.builder().namespace("golden").id("tall")
                .canvasWidth(300).canvasHeight(ScreenAssets.BACKDROP_MAX_HEIGHT + 1)
                .backdrop(List.of(Node.Box.builder().style("panel").width(300).height(ScreenAssets.BACKDROP_MAX_HEIGHT + 1).build()))
                .build();

        assertTrue(screens.validate(screen).stream().anyMatch(problem -> problem.contains("with a backdrop")));
    }

    @Test
    @DisplayName("a set action changes state and re-renders the other case")
    void setActionSwitchesCase() {
        screens.open(player, "golden:shop", Map.of(), Map.of("buy", context -> ActionResult.none()));
        assertTrue(bodyShows("Items"));

        sessionsClick("Perks");

        assertEquals("perks", screens.state(player).get("tab"));
    }

    @Test
    @DisplayName("a call action reaches the bound handler with arguments from the repeat entry")
    void callActionGetsRepeatArgs() {
        final AtomicReference<Object> bought = new AtomicReference<>();
        screens.open(player, "golden:shop",
                Map.of("items", List.of(Map.of("id", "sword", "name", "Sword", "price", 10.0))),
                Map.of("buy", context -> {
                    bought.set(context.arg("id"));
                    return ActionResult.update();
                }));

        sessionsClick("Sword");
        scheduled.forEach(Runnable::run);

        assertEquals("sword", bought.get());
    }

    @Test
    @DisplayName("an error result puts the translated message in state and the screen shows it")
    void errorResult() {
        screens.open(player, "golden:shop",
                Map.of("items", List.of(Map.of("id", "sword", "name", "Sword", "price", 10.0))),
                Map.of("buy", context -> ActionResult.error("core.dialog.test.name")));

        sessionsClick("Sword");
        scheduled.forEach(Runnable::run);

        assertTrue(screens.state(player).get("error") instanceof Component);
    }

    @Test
    @DisplayName("open stacks a screen and back returns to the one below")
    void openAndBack() {
        final ScreenDefinition first = ScreenDefinition.builder().namespace("golden").id("first").canvasWidth(200).canvasHeight(40)
                .element(Node.Text.builder().text(TextSpec.of("Go")).onClick(new ActionSpec.Open("second", Map.of("from", "first"))).build())
                .build();
        final ScreenDefinition second = ScreenDefinition.builder().namespace("golden").id("second").canvasWidth(200).canvasHeight(40)
                .element(Node.Text.builder().text(TextSpec.of("Return")).onClick(new ActionSpec.Back()).build())
                .build();
        screens.register(first);
        screens.register(second);
        screens.open(player, "golden:first", Map.of(), Map.of());

        sessionsClick("Go");
        assertEquals("first", screens.state(player).get("from"));
        sessionsClick("Return");
        assertNull(screens.state(player).get("from"));
        assertTrue(bodyShows("Go"));
    }

    private void sessionsClick(String label) {
        sessions.handle(player, click(label), DialogInputs.EMPTY);
    }
}
