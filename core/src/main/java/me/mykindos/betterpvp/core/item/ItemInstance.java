package me.mykindos.betterpvp.core.item;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializationRegistry;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import me.mykindos.betterpvp.core.item.renderer.ItemLoreRenderer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents an instance of an item, including its components and metadata.
 * This class is used to manage the state and behavior of items in the game.
 *
 * <h3>Automatic Component Serialization</h3>
 * <p>Components are automatically serialized to and deserialized from the ItemStack's
 * persistent data container whenever they are added or removed. This ensures that:</p>
 * <ul>
 *   <li>Component state is always synchronized with the ItemStack</li>
 *   <li>Items persist correctly when saved/loaded</li>
 *   <li>No manual serialization calls are needed when modifying components</li>
 * </ul>
 *
 * <p>The serialization uses the {@link ComponentSerializationRegistry} to find appropriate
 * serializers for each component type. Components that don't have registered serializers
 * will not be persisted to the ItemStack.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Components are automatically serialized when added
 * itemInstance = itemInstance.addComponent(new UUIDProperty());
 *
 * // The ItemStack now contains the serialized component data
 * ItemStack stack = itemInstance.getItemStack();
 *
 * // When creating from an ItemStack, components are automatically deserialized
 * Optional<ItemInstance> loaded = itemFactory.fromItemStack(stack);
 * }</pre>
 */
public class ItemInstance implements Item {

    private static final NamespacedKey COMPONENTS_KEY = new NamespacedKey("core", "components");

    @Getter
    private final @NotNull BaseItem baseItem;
    private final @NotNull ItemStack itemStack;
    private final @NotNull ComponentSerializationRegistry serializationRegistry;
    private final @NotNull ImmutableMap<Class<?>, ItemComponent> components;
    private final ItemInstanceView instanceView;

    ItemInstance(@NotNull BaseItem baseItem, @NotNull ItemStack itemStack, @NotNull ComponentSerializationRegistry serializationRegistry) {
        this(baseItem, itemStack, serializationRegistry, new HashMap<>());
    }

    private ItemInstance(@NotNull BaseItem baseItem, @NotNull ItemStack itemStack,
                         @NotNull ComponentSerializationRegistry serializationRegistry,
                         @NotNull Map<Class<?>, ItemComponent> components) {
        this.baseItem = baseItem;
        this.itemStack = itemStack;
        this.serializationRegistry = serializationRegistry;
        this.instanceView = new ItemInstanceView(this);

        // Build a mutable multimap for initialization
        Map<Class<?>, ItemComponent> mutableComponents = new HashMap<>();
        // Copy components from BaseItem
        for (ItemComponent component : baseItem.getComponents()) {
            final ItemComponent copy = component.copy();
            mutableComponents.put(copy.getClass(), copy);
        }

        // Deserialize additional components from the ItemStack
        deserializeComponentsFromItemStack(mutableComponents);

        // Overwrite with provided components
        mutableComponents.putAll(components);

        // Create an immutable view
        this.components = ImmutableMap.copyOf(mutableComponents);
    }

    public ItemInstanceView getView() {
        return instanceView;
    }

    public @NotNull ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Creates an ItemStack from the item instance.
     * @return a clone of the ItemStack representing the item instance.
     */
    public @NotNull ItemStack createItemStack() {
        return itemStack.clone();
    }

    /**
     * Adds a new component to this item instance, creating a new instance.
     * If the component already exists, it will be replaced.
     * @param component the component to add
     * @return a new ItemInstance with the added component
     */
    @Contract(pure = true, value = "_ -> new")
    public @NotNull ItemInstance withComponent(@NotNull ItemComponent component) {
        // Create a mutable copy of the components multimap
        Map<Class<?>, ItemComponent> newComponents = new HashMap<>(components);
        newComponents.put(component.getClass(), component);

        // Create a new instance with the updated components
        ItemInstance newInstance = new ItemInstance(baseItem, itemStack.clone(), serializationRegistry, newComponents);

        // Serialize the component to the new instance's ItemStack
        newInstance.serializeComponentToItemStack(component);

        return newInstance;
    }

    /**
     * Removes a component from this item instance, creating a new instance.
     * <br>
     * NOTE: Some components are not removable, such as those that are essential to the item,
     * these are defined in the BaseItem class and are automatically added to all instances.
     *
     * @param component the component to remove
     * @return a new ItemInstance without the specified component
     */
    @Contract(pure = true, value = "_ -> new")
    public @NotNull ItemInstance removeComponent(@NotNull ItemComponent component) {
        if (!components.containsKey(component.getClass())) {
            return this;
        }

        // Create a mutable copy of the components multimap
        Map<Class<?>, ItemComponent> newComponents = new HashMap<>(components);
        newComponents.remove(component.getClass(), component);

        // Create an exact copy of the current item instance
        ItemInstance newInstance = new ItemInstance(baseItem, itemStack, serializationRegistry, newComponents);

        // Remove the component from the new instance's ItemStack
        newInstance.removeComponentFromItemStack(component);

        return newInstance;
    }

    @Override
    public @NotNull Set<ItemComponent> getComponents() {
        return new HashSet<>(components.values());
    }

    @Override
    public @Nullable ItemLoreRenderer getLoreRenderer() {
        return baseItem.getLoreRenderer();
    }

    public @NotNull ItemRarity getRarity() {
        return baseItem.getInstanceRarityProvider().apply(this);
    }

    private void deserializeComponentsFromItemStack(Map<Class<?>, ItemComponent> targetComponents) {
        // Try to deserialize each registered component type
        withComponentsContainer(container -> {
            for (Map.Entry<NamespacedKey, ComponentDeserializer<?>> entry : serializationRegistry.getAllDeserializers().entrySet()) {
                if (!entry.getValue().hasData(container)) {
                    continue;
                }

                ItemComponent component = entry.getValue().deserialize(this, container);
                targetComponents.put(component.getClass(), component);
            }
        });
    }

    public void serializeAllComponentsToItemStack() {
        // Serialize only the components that should be serialized according to the BaseItem
        updateComponentsContainer(container -> {
            for (ItemComponent component : components.values()) {
                if (baseItem.shouldSerializeComponent(component)) {
                    serializeComponentToContainer(component, container);
                }
            }
        });
    }

    private void serializeComponentToItemStack(ItemComponent component) {
        // Only serialize if the BaseItem allows it
        if (baseItem.shouldSerializeComponent(component)) {
            updateComponentsContainer(container -> {
                serializeComponentToContainer(component, container);
            });
        }
    }

    private void removeComponentFromItemStack(ItemComponent component) {
        // Only need to remove from ItemStack if it was serialized
        if (baseItem.shouldSerializeComponent(component)) {
            updateComponentsContainer(container -> {
                // Find the serializer for this component and remove its data
                Optional<ComponentSerializer<ItemComponent>> serializerOpt = getSerializer(component);
                serializerOpt.ifPresent(serializer -> serializer.delete(component, container));
            });
        }
    }

    /**
     * Executes an operation with the existing components container (read-only).
     * Does nothing if no components container exists.
     */
    private void withComponentsContainer(Consumer<PersistentDataContainer> operation) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer root = meta.getPersistentDataContainer();
        if (!root.has(COMPONENTS_KEY, PersistentDataType.TAG_CONTAINER)) {
            return;
        }

        PersistentDataContainer componentsContainer = root.get(COMPONENTS_KEY, PersistentDataType.TAG_CONTAINER);
        if (componentsContainer != null) {
            operation.accept(componentsContainer);
        }
    }

    /**
     * Executes an operation with a components container, creating one if needed.
     * Automatically saves changes back to the ItemStack.
     */
    private void updateComponentsContainer(Consumer<PersistentDataContainer> operation) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer root = meta.getPersistentDataContainer();

        // Get or create components container
        PersistentDataContainer componentsContainer;
        if (root.has(COMPONENTS_KEY, PersistentDataType.TAG_CONTAINER)) {
            componentsContainer = root.get(COMPONENTS_KEY, PersistentDataType.TAG_CONTAINER);
        } else {
            componentsContainer = root.getAdapterContext().newPersistentDataContainer();
        }

        if (componentsContainer != null) {
            operation.accept(componentsContainer);

            // Update the root container - remove if empty, otherwise save
            if (componentsContainer.isEmpty() && root.has(COMPONENTS_KEY, PersistentDataType.TAG_CONTAINER)) {
                root.remove(COMPONENTS_KEY);
            } else {
                root.set(COMPONENTS_KEY, PersistentDataType.TAG_CONTAINER, componentsContainer);
            }
            itemStack.setItemMeta(meta);
        }
    }

    @SuppressWarnings("unchecked")
    private void serializeComponentToContainer(ItemComponent component, PersistentDataContainer container) {
        if (!baseItem.shouldSerializeComponent(component)) {
            return;
        }

        Optional<? extends ComponentSerializer<? extends ItemComponent>> serializerOpt = serializationRegistry.getSerializer(component.getClass());
        if (serializerOpt.isPresent()) {
            ComponentSerializer<ItemComponent> serializer = (ComponentSerializer<ItemComponent>) serializerOpt.get();
            serializer.serialize(component, container);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<ComponentSerializer<ItemComponent>> getSerializer(ItemComponent component) {
        Optional<? extends ComponentSerializer<? extends ItemComponent>> serializerOpt = serializationRegistry.getSerializer(component.getClass());
        return serializerOpt.map(serializer -> (ComponentSerializer<ItemComponent>) serializer);
    }
}