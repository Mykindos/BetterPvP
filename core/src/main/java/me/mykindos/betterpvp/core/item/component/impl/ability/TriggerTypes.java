package me.mykindos.betterpvp.core.item.component.impl.ability;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a trigger type for an ability.
 */
public enum TriggerTypes implements TriggerType {

    /**
     * Triggered when the item is right-clicked.
     */
    RIGHT_CLICK("RIGHT-CLICK"),

    /**
     * Triggered when the item is shift-right-clicked.
     */
    SHIFT_RIGHT_CLICK("SHIFT-CLICK"),

    /**
     * Triggered when the item is left-clicked.
     */
    LEFT_CLICK("LEFT-CLICK"),

    /**
     * Triggered when the item is shift-left-clicked.
     */
    SHIFT_LEFT_CLICK("SHIFT-CLICK"),
    
    /**
     * Triggered when the item is being held with a right-click.
     */
    HOLD_RIGHT_CLICK("RIGHT-CLICK"),

    /**
     * Triggered when the item is being held with right-click and spawns a shield to block the user from
     * clicking.
     */
    HOLD_BLOCK("RIGHT-CLICK"),

    /**
     * Triggered when the item is being held
     */
    HOLD("HOLD"),

    /**
     * Triggered when the player presses the off-hand key while holding the item.
     */
    OFF_HAND(Component.keybind("key.swapOffhand").append(Component.text(" KEY"))),

    /**
     * Not triggered by any action. Effect is delegated to the ability class.
     */
    PASSIVE("PASSIVE");

    @Getter
    private final Component name;

    TriggerTypes(@NotNull String name) {
        this.name = Component.text(name);
    }

    TriggerTypes(@NotNull Component component) {
        this.name = component;
    }
}
