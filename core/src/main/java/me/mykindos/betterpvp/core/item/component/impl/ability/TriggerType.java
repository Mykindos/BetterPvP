package me.mykindos.betterpvp.core.item.component.impl.ability;

import lombok.Getter;

/**
 * Represents a trigger type for an ability.
 */
public enum TriggerType {

    /**
     * Triggered when the item is right-clicked.
     */
    RIGHT_CLICK("Right-Click"),

    /**
     * Triggered when the item is left-clicked.
     */
    LEFT_CLICK("Left-Click"),

    /**
     * Triggered when the item is clicked while holding the shift key.
     */
    SHIFT_CLICK("Shift-Click"),
    
    /**
     * Triggered when the item is being held with a right-click.
     */
    HOLD_RIGHT_CLICK("Right-Click"),

    /**
     * Triggered when the item is being held with right-click and spawns a shield to block the user from
     * clicking.
     */
    HOLD_BLOCK("Right-Click"),

    /**
     * Triggered when the item is being held
     */
    HOLD("Hold"),

    /**
     * Not triggered by any action. Effect is delegated to the ability class.
     */
    PASSIVE("Passive");

    @Getter
    private final String name;

    TriggerType(String name) {
        this.name = name;
    }
}
