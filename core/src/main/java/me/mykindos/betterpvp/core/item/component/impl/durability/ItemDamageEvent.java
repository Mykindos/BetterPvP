package me.mykindos.betterpvp.core.item.component.impl.durability;

import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;

/**
 * Event that is triggered when an item takes damage.
 */
public class ItemDamageEvent extends CustomCancellableEvent {

    private final ItemInstance itemInstance;
    private final DurabilityComponent durabilityComponent;
    private int damage;

    public ItemDamageEvent(ItemInstance itemInstance, DurabilityComponent durabilityComponent, int damage) {
        this.itemInstance = itemInstance;
        this.durabilityComponent = durabilityComponent;
        this.damage = damage;
    }

    public boolean willBreak() {
        return durabilityComponent.getDamage() + damage >= durabilityComponent.getMaxDamage();
    }

    public ItemInstance getItem() {
        return itemInstance;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }

    public DurabilityComponent copyComponent() {
        return durabilityComponent.copy();
    }
}
