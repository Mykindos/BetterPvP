package me.mykindos.betterpvp.core.item.pagination;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LorePageServiceTest {

    private LorePageService service;
    private final UUID viewer = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LorePageService();
    }

    private static ItemComponent loreComponent(int page) {
        ItemComponent component = mock(ItemComponent.class, withSettings().extraInterfaces(LoreComponent.class));
        LoreComponent lore = (LoreComponent) component;
        when(lore.getLorePage()).thenReturn(page);
        when(lore.getLines(any())).thenReturn(List.of(Component.text("line")));
        return component;
    }

    private static List<ItemComponent> threePages() {
        return List.of(loreComponent(0), loreComponent(1), loreComponent(2));
    }

    /** A three-page item (0,1,2) with no UUID; identity falls back to its ItemStack. */
    private static ItemInstance threePageItem(ItemStack identityStack) {
        final List<ItemComponent> components = threePages();
        ItemInstance item = mock(ItemInstance.class);
        when(item.getComponents()).thenReturn(components);
        doReturn(Optional.empty()).when(item).getComponent(any());
        when(item.getItemStack()).thenReturn(identityStack);
        return item;
    }

    private static ItemInstance uuidItem(UUID id) {
        final List<ItemComponent> components = threePages();
        ItemInstance item = mock(ItemInstance.class);
        when(item.getComponents()).thenReturn(components);
        doReturn(Optional.of(new UUIDProperty(id))).when(item).getComponent(UUIDProperty.class);
        return item;
    }

    @Test
    @DisplayName("resolve defaults to the most relevant page when nothing is stored")
    void resolveDefaultsToMostRelevant() {
        ItemInstance item = threePageItem(mock(ItemStack.class));
        assertEquals(0, service.resolve(viewer, item));
    }

    @Test
    @DisplayName("advance cycles 0 -> 1 -> 2 -> 0")
    void advanceCycles() {
        ItemInstance item = uuidItem(UUID.randomUUID());
        assertEquals(0, service.resolve(viewer, item));
        service.advance(viewer, item);
        assertEquals(1, service.resolve(viewer, item));
        service.advance(viewer, item);
        assertEquals(2, service.resolve(viewer, item));
        service.advance(viewer, item);
        assertEquals(0, service.resolve(viewer, item));
    }

    @Test
    @DisplayName("Two instances with the same UUID share page state")
    void sameUuidSharesState() {
        UUID id = UUID.randomUUID();
        ItemInstance first = uuidItem(id);
        ItemInstance second = uuidItem(id);

        service.advance(viewer, first);
        assertEquals(1, service.resolve(viewer, second));
    }

    @Test
    @DisplayName("Different UUIDs do not share page state")
    void differentUuidIndependent() {
        ItemInstance first = uuidItem(UUID.randomUUID());
        ItemInstance second = uuidItem(UUID.randomUUID());

        service.advance(viewer, first);
        assertEquals(1, service.resolve(viewer, first));
        assertEquals(0, service.resolve(viewer, second));
    }

    @Test
    @DisplayName("UUID-less items with equal content (same stack) share state; different content does not")
    void contentHashFallback() {
        ItemStack stack = mock(ItemStack.class);
        ItemInstance a = threePageItem(stack);
        ItemInstance b = threePageItem(stack);       // same identity object
        ItemInstance other = threePageItem(mock(ItemStack.class)); // different identity object

        service.advance(viewer, a);
        assertEquals(1, service.resolve(viewer, b));
        assertEquals(0, service.resolve(viewer, other));
    }

    @Test
    @DisplayName("Different viewers track pages independently")
    void viewersIndependent() {
        UUID otherViewer = UUID.randomUUID();
        ItemInstance item = uuidItem(UUID.randomUUID());

        service.advance(viewer, item);
        assertEquals(1, service.resolve(viewer, item));
        assertEquals(0, service.resolve(otherViewer, item));
        assertNotEquals(service.resolve(viewer, item), service.resolve(otherViewer, item));
    }
}
