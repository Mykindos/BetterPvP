package me.mykindos.betterpvp.core.command.menus;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.inventory.event.ItemPostUpdateEvent;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Logs UUID items a viewer moves in or out of another player's inventory, whether that player is online or not.
 */
@CustomLog
class InvseeItemLogger {

    private final ItemRegistry registry;
    private final ComponentLookupService lookupService;

    InvseeItemLogger(ItemRegistry registry, ComponentLookupService lookupService) {
        this.registry = registry;
        this.lookupService = lookupService;
    }

    void log(Player viewer, UUID targetId, String targetName, ItemPostUpdateEvent event) {
        lookupService.getItemComponentPair(event.getNewItem(), UUIDProperty.class).ifPresent(result -> {
            log.info("{} put ({}) in {}'s inventory", viewer.getName(), result.getComponent().getUniqueId().toString(), targetName)
                    .setAction("ITEM_INVSEE_PUT")
                    .addItemContext(registry, result.getItem())
                    .addClientContext(viewer)
                    .addClientContext(targetId, targetName, true)
                    .submit();
        });

        lookupService.getItemComponentPair(event.getPreviousItem(), UUIDProperty.class).ifPresent(result -> {
            log.info("{} retrieved ({}) from {}'s inventory", viewer.getName(), result.getComponent().getUniqueId().toString(), targetName)
                    .setAction("ITEM_INVSEE_RETRIEVE")
                    .addItemContext(registry, result.getItem())
                    .addClientContext(viewer)
                    .addClientContext(targetId, targetName, true)
                    .submit();
        });
    }
}
