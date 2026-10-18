package me.mykindos.betterpvp.core.command.menus;

import me.mykindos.betterpvp.core.inventory.inventory.Inventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.ItemPostUpdateEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.logging.LogAppender;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.LoggerFactory;
import me.mykindos.betterpvp.core.logging.PendingLog;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Invsee item logging")
class InvseeItemLoggerTest {

    private static final UUID TARGET_ID = UUID.nameUUIDFromBytes("Bob".getBytes(StandardCharsets.UTF_8));
    private static final UUID ITEM_ID = UUID.nameUUIDFromBytes("sword".getBytes(StandardCharsets.UTF_8));

    private final ItemRegistry registry = mock(ItemRegistry.class);
    private final ComponentLookupService lookupService = mock(ComponentLookupService.class);
    private final List<PendingLog> logs = new CopyOnWriteArrayList<>();
    private final LogAppender appender = logs::add;

    @AfterEach
    void removeAppender() {
        LoggerFactory.getInstance().removeAppender(appender);
    }

    private ItemStack uuidItem() {
        final ItemStack stack = mock(ItemStack.class);
        final ItemInstance item = mock(ItemInstance.class);
        final UUIDProperty property = mock(UUIDProperty.class);
        final BaseItem baseItem = mock(BaseItem.class);
        when(property.getUniqueId()).thenReturn(ITEM_ID);
        when(item.getComponent(UUIDProperty.class)).thenReturn(Optional.of(property));
        when(item.getBaseItem()).thenReturn(baseItem);
        when(registry.getKey(baseItem)).thenReturn(new NamespacedKey("core", "sword"));
        when(lookupService.getItemComponentPair(stack, UUIDProperty.class))
                .thenReturn(Optional.of(new ComponentLookupService.Result<>(item, property)));
        return stack;
    }

    private PendingLog awaitLog(String action) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            final Optional<PendingLog> log = logs.stream().filter(pending -> pending.getAction().equals(action)).findFirst();
            if (log.isPresent()) {
                return log.get();
            }
            Thread.sleep(10);
        }
        throw new AssertionError("No " + action + " log, got " + logs);
    }

    private static void assertNamesBoth(PendingLog log) {
        final Map<String, String> context = log.getContext();
        assertAll(
                () -> assertEquals("Alice", context.get(LogContext.CLIENT_NAME)),
                () -> assertEquals(TARGET_ID.toString(), context.get(LogContext.TARGET_CLIENT)),
                () -> assertEquals("Bob", context.get(LogContext.TARGET_CLIENT_NAME)),
                () -> assertEquals(ITEM_ID.toString(), context.get(LogContext.ITEM)));
    }

    private static Player viewer() {
        final Player viewer = mock(Player.class);
        when(viewer.getName()).thenReturn("Alice");
        when(viewer.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes("Alice".getBytes(StandardCharsets.UTF_8)));
        return viewer;
    }

    @Test
    @DisplayName("#2401 AC2 putting a UUID item in logs ITEM_INVSEE_PUT naming the target by id and name, without a Player")
    void ac2_putLoggedByTargetIdAndName() throws InterruptedException {
        LoggerFactory.getInstance().addAppender(appender);
        final InvseeItemLogger logger = new InvseeItemLogger(registry, lookupService);
        final ItemPostUpdateEvent event = new ItemPostUpdateEvent(mock(Inventory.class), 0, null, null, uuidItem());

        assertDoesNotThrow(() -> logger.log(viewer(), TARGET_ID, "Bob", event));

        assertNamesBoth(awaitLog("ITEM_INVSEE_PUT"));
    }

    @Test
    @DisplayName("#2401 AC2 taking a UUID item out logs ITEM_INVSEE_RETRIEVE naming the target by id and name, without a Player")
    void ac2_retrieveLoggedByTargetIdAndName() throws InterruptedException {
        LoggerFactory.getInstance().addAppender(appender);
        final InvseeItemLogger logger = new InvseeItemLogger(registry, lookupService);
        final ItemPostUpdateEvent event = new ItemPostUpdateEvent(mock(Inventory.class), 0, null, uuidItem(), null);

        assertDoesNotThrow(() -> logger.log(viewer(), TARGET_ID, "Bob", event));

        assertNamesBoth(awaitLog("ITEM_INVSEE_RETRIEVE"));
    }
}
