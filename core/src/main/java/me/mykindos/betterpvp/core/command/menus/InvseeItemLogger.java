package me.mykindos.betterpvp.core.command.menus;

import me.mykindos.betterpvp.core.inventory.inventory.event.ItemPostUpdateEvent;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Logs UUID items a viewer moves in or out of another player's inventory, whether that player is online or not.
 */
class InvseeItemLogger {

    private final ItemRegistry registry;
    private final ComponentLookupService lookupService;

    InvseeItemLogger(ItemRegistry registry, ComponentLookupService lookupService) {
        this.registry = registry;
        this.lookupService = lookupService;
    }

    void log(Player viewer, UUID targetId, String targetName, ItemPostUpdateEvent event) {
        throw new UnsupportedOperationException("#2401");
    }
}
