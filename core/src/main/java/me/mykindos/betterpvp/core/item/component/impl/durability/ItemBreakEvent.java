package me.mykindos.betterpvp.core.item.component.impl.durability;

import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;

/**
 * Event that is triggered when an item takes damage.
 */
public class ItemBreakEvent extends CustomEvent {

    private final ItemInstance itemInstance;
    private final DurabilityComponent durabilityComponent;

    public ItemBreakEvent(ItemInstance itemInstance, DurabilityComponent durabilityComponent) {
        this.itemInstance = itemInstance;
        this.durabilityComponent = durabilityComponent;
    }

    public ItemInstance getItem() {
        return itemInstance;
    }

    public DurabilityComponent copyComponent() {
        return durabilityComponent.copy();
    }
}
