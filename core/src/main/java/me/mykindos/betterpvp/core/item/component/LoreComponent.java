package me.mykindos.betterpvp.core.item.component;

import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Represents a component that can be added to an item's lore.
 */
public interface LoreComponent {

    /**
     * Get the lines of lore for this component.
     *
     * @param item The item this component is applied to
     * @return The lines of lore for this component
     */
    List<Component> getLines(ItemInstance item);

    /**
     * Get the rendering order priority of this component.
     * Lower numbers render first.
     */
    int getRenderPriority();

}
