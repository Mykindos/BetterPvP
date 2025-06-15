package me.mykindos.betterpvp.core.item.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Service that provides functionality for retrieving and working with
 * item components from ItemStacks and ItemInstances.
 * <p>
 * This service simplifies common operations like finding items with specific
 * components or retrieving typed component information directly from ItemStacks.
 */
@Singleton
public class ComponentLookupService {

    private final ItemFactory itemFactory;

    @Inject
    public ComponentLookupService(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    /**
     * Gets an ItemInstance with the specified component type from an ItemStack, if present.
     *
     * @param itemStack The ItemStack to check
     * @param componentClass The component type to look for
     * @param <T> The type of component
     * @return Optional containing the ItemInstance if found with the component, otherwise empty
     */
    public <T extends ItemComponent> Optional<ItemInstance> getItemWithComponent(
            @Nullable ItemStack itemStack,
            @NotNull Class<T> componentClass) {

        if (itemStack == null) {
            return Optional.empty();
        }

        Optional<ItemInstance> itemInstance = itemFactory.fromItemStack(itemStack);
        if (itemInstance.isEmpty()) {
            return Optional.empty();
        }

        // Check if the item has the specified component
        if (itemInstance.get().getComponent(componentClass).isPresent()) {
            return itemInstance;
        }

        return Optional.empty();
    }

    /**
     * Gets a component of the specified type from an ItemStack, if present.
     *
     * @param itemStack The ItemStack to check
     * @param componentClass The component type to look for
     * @param <T> The type of component
     * @return Optional containing the component if found, otherwise empty
     */
    public <T extends ItemComponent> Optional<T> getComponent(
            @Nullable ItemStack itemStack,
            @NotNull Class<T> componentClass) {

        if (itemStack == null) {
            return Optional.empty();
        }

        Optional<ItemInstance> itemInstance = itemFactory.fromItemStack(itemStack);
        if (itemInstance.isEmpty()) {
            return Optional.empty();
        }

        return itemInstance.get().getComponent(componentClass);
    }

    /**
     * Convenience method to get an item with a specific component and provide both
     * the item instance and the component in a tuple-like object.
     *
     * @param itemStack The ItemStack to check
     * @param componentClass The component type to look for
     * @param <T> The type of component
     * @return Optional containing the ItemComponentPair if found, otherwise empty
     */
    public <T extends ItemComponent> Optional<Result<T>> getItemComponentPair(
            @Nullable ItemStack itemStack,
            @NotNull Class<T> componentClass) {

        Optional<ItemInstance> itemOpt = getItemWithComponent(itemStack, componentClass);
        if (itemOpt.isEmpty()) {
            return Optional.empty();
        }

        ItemInstance item = itemOpt.get();
        Optional<T> componentOpt = item.getComponent(componentClass);

        return componentOpt.map(t -> new Result<>(item, t));
    }

    /**
     * Container class that pairs an ItemInstance with a specific component.
     * Useful for operations that need both the item and its component.
     *
     * @param <T> The type of component
     */
    public record Result<T extends ItemComponent>(@Getter ItemInstance item, @Getter T component) {
        // This record pairs an ItemInstance with a specific component type.
        // It provides getters for both the item and the component.
    }
}