package me.mykindos.betterpvp.core.item;

import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.renderer.ItemLoreRenderer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Interface representing an item in the game.
 * This interface allows for dynamic addition and removal of components,
 * enabling extensibility through the Component Pattern.
 */
@SuppressWarnings("UnstableApiUsage")
public interface Item {

    static ModelBuilder builder(@NotNull String model) {
        return builder(Material.FERMENTED_SPIDER_EYE).model("item/" + model);
    }

    static ModelBuilder builder(@NotNull Material material, @NotNull String model) {
        return builder(material).model(model);
    }

    /**
     * Creates a new ItemBuilder with the specified material.
     *
     * @param material The material for the item
     * @return A new ItemBuilder instance
     */
    static ModelBuilder builder(@NotNull Material material) {
        return new ModelBuilder(material, new ItemStack(material));
    }

    static ItemStack model(Material material, @Subst("test") String model, int stackSize) {
        return builder(material)
                .model("item/" + model)
                .maxStackSize(stackSize)
                .build();
    }

    static ItemStack model(Material material, @Subst("test") String model) {
        return builder(material)
                .model("item/" + model)
                .build();
    }

    static ItemStack model(@Subst("test") String model, int stackSize) {
        return model(Material.FERMENTED_SPIDER_EYE, model, stackSize);
    }

    static ItemStack model(@Subst("test") String model) {
        return builder(model).build();
    }

    /**
     * Get a single, distinct, component of a specific type, local to this instance only.
     * This does not include components from parents.
     *
     * @param componentClass The class of the component to get
     * @return The component of the specified type, or null if not found
     */
    default <T extends ItemComponent> Optional<T> getComponent(@NotNull Class<T> componentClass) {
        return getComponents().stream()
                .filter(componentClass::isInstance)
                .map(componentClass::cast)
                .findFirst();
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
