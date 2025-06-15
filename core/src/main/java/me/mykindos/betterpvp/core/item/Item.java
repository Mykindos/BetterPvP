package me.mykindos.betterpvp.core.item;

import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.renderer.ItemLoreRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Interface representing an item in the game.
 * This interface allows for dynamic addition and removal of components,
 * enabling extensibility through the Component Pattern.
 */
public interface Item {

    /**
     * Get all components of a specific type, local to this instance only.
     * This does not include components from parents.
     *
     * @param componentClass The class of the component to get
     * @return A set of components of the specified type
     */
    <T extends ItemComponent> Set<T> getComponents(@NotNull Class<T> componentClass);

    /**
     * Get a single, distinct, component of a specific type, local to this instance only.
     * This does not include components from parents.
     *
     * @param componentClass The class of the component to get
     * @return The component of the specified type, or null if not found
     */
    default <T extends ItemComponent> Optional<T> getComponent(@NotNull Class<T> componentClass) {
        Set<T> components = getComponents(componentClass);
        if (components.isEmpty()) {
            return Optional.empty();
        }
        if (components.size() > 1) {
            throw new IllegalStateException("Multiple components of type " + componentClass.getName() + " found");
        }
        return Optional.ofNullable(components.iterator().next());
    }

    /**
     * Get all components of this item, local to this instance only.
     * This does not include components from parents.
     *
     * @return A set of all components
     */
    @NotNull Set<@NotNull ItemComponent> getComponents();
    /**
     * @return The lore renderer for this item, or null if none is set
     */
    @Nullable ItemLoreRenderer getLoreRenderer();

}
