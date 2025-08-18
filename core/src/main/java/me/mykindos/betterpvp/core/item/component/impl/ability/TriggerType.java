package me.mykindos.betterpvp.core.item.component.impl.ability;

import net.kyori.adventure.text.Component;

/**
 * Represents a trigger type for an ability.
 *
 * @see TriggerType TriggerType, for default implementations */
public interface TriggerType {

    static TriggerType dummy(String name) {
        return () -> Component.text(name);
    }

    /**
     * Returns the name of the trigger type.
     * @return the name of the trigger type
     */
    Component getName();

}
