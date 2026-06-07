package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoreComponentRendererPageTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        // Compatibility (TEXTURE_PROVIDER) reads Bukkit.getPluginManager() at class-init; keep it server-free.
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private static ItemComponent loreComponent(int page, int priority, String... lines) {
        ItemComponent component = mock(ItemComponent.class, withSettings().extraInterfaces(LoreComponent.class));
        LoreComponent lore = (LoreComponent) component;
        when(lore.getLorePage()).thenReturn(page);
        when(lore.getRenderPriority()).thenReturn(priority);
        when(lore.getLines(any())).thenReturn(Arrays.stream(lines).map(s -> (Component) Component.text(s)).toList());
        return component;
    }

    private static ItemInstance itemWith(ItemComponent... components) {
        ItemInstance item = mock(ItemInstance.class);
        when(item.getComponents()).thenReturn(List.of(components));
        when(item.getRarity()).thenReturn(ItemRarity.COMMON);
        doReturn(Optional.empty()).when(item).getComponent(any());
        return item;
    }

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(PLAIN::serialize).toList();
    }

    @Test
    @DisplayName("Only the requested page's component lines are rendered")
    void rendersOnlyRequestedPage() {
        ItemInstance item = itemWith(
                loreComponent(0, 0, "story"),
                loreComponent(1, 0, "socket-line"),
                loreComponent(2, 0, "ability-line"));

        List<String> page1 = plain(new LoreComponentRenderer().create(item, null, 1));

        assertTrue(page1.contains("socket-line"), "page 1 should contain socket content");
        assertFalse(page1.contains("story"), "page 1 should exclude page 0");
        assertFalse(page1.contains("ability-line"), "page 1 should exclude page 2");
    }

    @Test
    @DisplayName("Within a page, components keep render-priority ordering")
    void keepsPriorityOrderWithinPage() {
        ItemInstance item = itemWith(
                loreComponent(0, 100, "low-priority"),
                loreComponent(0, 1, "high-priority"));

        List<String> page0 = plain(new LoreComponentRenderer().create(item, null, 0));
        assertTrue(page0.indexOf("high-priority") < page0.indexOf("low-priority"));
    }

    @Test
    @DisplayName("No footer or page indicator line is added")
    void noFooterAdded() {
        ItemInstance item = itemWith(
                loreComponent(0, 0, "story"),
                loreComponent(1, 0, "socket-line"));

        List<String> page0 = plain(new LoreComponentRenderer().create(item, null, 0));
        assertTrue(page0.contains("story"));
        assertFalse(page0.stream().anyMatch(line -> line.toLowerCase().contains("page")));
    }
}
