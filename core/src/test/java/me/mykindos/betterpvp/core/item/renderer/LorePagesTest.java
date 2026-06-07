package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LorePagesTest {

    /**
     * Builds a component that is both an {@link ItemComponent} (so it sits in getComponents())
     * and a {@link LoreComponent} (so the renderer/LorePages picks it up).
     */
    private static ItemComponent loreComponent(int page, List<Component> lines) {
        ItemComponent component = mock(ItemComponent.class, withSettings().extraInterfaces(LoreComponent.class));
        LoreComponent lore = (LoreComponent) component;
        when(lore.getLorePage()).thenReturn(page);
        when(lore.getLines(any())).thenReturn(lines);
        return component;
    }

    private static List<Component> nonEmpty() {
        return List.of(Component.text("line"));
    }

    private static ItemInstance itemWith(ItemComponent... components) {
        ItemInstance item = mock(ItemInstance.class);
        when(item.getComponents()).thenReturn(List.of(components));
        return item;
    }

    @Test
    @DisplayName("A single component yields a single visible page")
    void singleComponentSinglePage() {
        ItemInstance item = itemWith(loreComponent(0, nonEmpty()));
        assertEquals(List.of(0), LorePages.visiblePages(item));
    }

    @Test
    @DisplayName("Story + sockets + abilities yield pages 0,1,2 sorted and distinct")
    void threePagesSortedDistinct() {
        ItemInstance item = itemWith(
                loreComponent(2, nonEmpty()),
                loreComponent(0, nonEmpty()),
                loreComponent(0, nonEmpty()),
                loreComponent(1, nonEmpty()));
        assertEquals(List.of(0, 1, 2), LorePages.visiblePages(item));
    }

    @Test
    @DisplayName("Components producing no lines are excluded from visible pages")
    void emptyComponentsExcluded() {
        ItemInstance item = itemWith(
                loreComponent(0, nonEmpty()),
                loreComponent(1, List.of()));
        assertEquals(List.of(0), LorePages.visiblePages(item));
    }

    @Test
    @DisplayName("Gaps in page numbers are preserved")
    void gapsPreserved() {
        ItemInstance item = itemWith(
                loreComponent(0, nonEmpty()),
                loreComponent(2, nonEmpty()));
        assertEquals(List.of(0, 2), LorePages.visiblePages(item));
    }

    @Test
    @DisplayName("mostRelevant is the lowest visible page")
    void mostRelevantIsLowest() {
        ItemInstance item = itemWith(
                loreComponent(2, nonEmpty()),
                loreComponent(1, nonEmpty()));
        assertEquals(1, LorePages.mostRelevant(item));
    }

    @Test
    @DisplayName("mostRelevant defaults to 0 when nothing is visible")
    void mostRelevantDefaultsToZero() {
        ItemInstance item = itemWith(loreComponent(0, List.of()));
        assertEquals(0, LorePages.mostRelevant(item));
        assertTrue(LorePages.visiblePages(item).isEmpty());
    }

    @Test
    @DisplayName("clamp returns the page when it is visible")
    void clampReturnsVisiblePage() {
        ItemInstance item = itemWith(
                loreComponent(0, nonEmpty()),
                loreComponent(2, nonEmpty()));
        assertEquals(2, LorePages.clamp(item, 2));
    }

    @Test
    @DisplayName("clamp snaps to the nearest visible page, ties resolve lower")
    void clampSnapsToNearest() {
        ItemInstance item = itemWith(
                loreComponent(0, nonEmpty()),
                loreComponent(4, nonEmpty()));
        // 1 is closer to 0; 3 is closer to 4; 2 ties -> lower (0)
        assertEquals(0, LorePages.clamp(item, 1));
        assertEquals(4, LorePages.clamp(item, 3));
        assertEquals(0, LorePages.clamp(item, 2));
        // beyond the top
        assertEquals(4, LorePages.clamp(item, 9));
    }
}
