package me.mykindos.betterpvp.core.item;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializationRegistry;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Factory for creating ItemInstance objects from BaseItems or ItemStacks.
 * Also provides builder methods for convenient item creation and editing.
 */
@Singleton
@CustomLog
public class ItemFactory {

    @Getter
    private final ItemRegistry itemRegistry;
    private final ComponentSerializationRegistry serializationRegistry;
    private final List<Function<ItemInstance, ItemInstance>> defaultBuilders = new ArrayList<>();

    @Inject
    private ItemFactory(ItemRegistry itemRegistry, ComponentSerializationRegistry serializationRegistry) {
        this.itemRegistry = itemRegistry;
        this.serializationRegistry = serializationRegistry;
    }

    /**
     * Registers a default builder that will be applied to all ItemInstances created by this factory.
     * This can be used to set common properties or components on all items.
     *
     * @param builder The consumer to apply to each ItemInstance
     */
    public void registerDefaultBuilder(@NotNull Consumer<@NotNull ItemInstance> builder) {
        registerDefaultBuilder(itemInstance -> {
            builder.accept(itemInstance);
            return itemInstance;
        });
    }

    /**
     * Registers a default builder that will be applied to all ItemInstances created by this factory.
     * This can be used to set common properties or components on all items.
     * @param builder The function to apply to each ItemInstance
     */
    public void registerDefaultBuilder(@NotNull Function<@NotNull ItemInstance, @NotNull ItemInstance> builder) {
        Preconditions.checkNotNull(builder, "Builder cannot be null");
        defaultBuilders.add(builder);
    }

    /**
     * Creates a new ItemInstance from a BaseItem (fresh instance). The fresh instance copies the
     * components from the BaseItem, so the BaseItem acts as a template for the ItemInstance.
     * Only components that the BaseItem says should be serialized will be persisted in the ItemStack.
     *
     * @param baseItem The base item to instantiate
     * @param builder A consumer to modify the ItemInstance after creation, which can be used to set additional properties like
     *                components or stats.
     * @return The new ItemInstance
     */
    @Contract(pure = true)
    public ItemInstance create(@NotNull BaseItem baseItem, @NotNull Consumer<@NotNull ItemInstance> builder) {
//        Preconditions.checkArgument(itemRegistry.isRegistered(baseItem), "BaseItem must be registered in the ItemRegistry");

        // Apply all builders and serialize components
        ItemInstance instance = new ItemInstance(baseItem, baseItem.getModel().clone(), serializationRegistry);
        for (Function<ItemInstance, ItemInstance> defaultBuilder : defaultBuilders) {
            instance = defaultBuilder.apply(instance);
        }
        instance.serializeAllComponentsToItemStack();

        // Add necessary versioning and custom item tags ONLY to items
        // that arent fallback with no custom data
        final ItemStack itemStack = instance.getItemStack();
        final BaseItem fallback = getFallbackItem(itemStack.getType());
        itemStack.editPersistentDataContainer(pdc -> {
            if (pdc.isEmpty() & fallback.equals(baseItem)) {
                // Fallback items do not need to be serialized
                // unless their PDC is not empty
                return;
            }

            pdc.set(CoreNamespaceKeys.BASEITEM_HASHCODE_KEY, PersistentDataType.INTEGER, baseItem.hashCode());

            // Some items do not have a key, like vanilla items.
            // These do not need to be serialized.
            final NamespacedKey key = itemRegistry.getKey(baseItem);
            if (key != null) {
                pdc.set(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING, key.toString());
            }
        });
        builder.accept(instance);
        return instance;
    }
    
    /**
     * Creates a new ItemInstance from a BaseItem (fresh instance).
     * @param baseItem The base item to instantiate
     * @return The new ItemInstance
     * @see #create(BaseItem, Consumer)
     */
    @Contract(pure = true)
    public ItemInstance create(@NotNull BaseItem baseItem) {
        return create(baseItem, itemInstance -> {
            // No additional actions needed
        });
    }
    
    /**
     * Reads an ItemStack and creates an ItemInstance for it, if possible.
     * Components are automatically deserialized by the ItemInstance constructor.
     * 
     * @param stack The ItemStack to read
     * @return The ItemInstance, or empty if not a recognized custom item
     */
    @Contract(pure = true)
    public Optional<ItemInstance> fromItemStack(@NotNull ItemStack stack) {
        if (stack.getType() == Material.AIR) {
            return Optional.empty(); // No item to read
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.of(getFallbackInstance(stack));
        }
        String id = meta.getPersistentDataContainer().get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
        if (id == null) {
            return Optional.of(getFallbackInstance(stack));
        }
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) {
            return Optional.of(getFallbackInstance(stack));
        }
        BaseItem baseItem = itemRegistry.getItem(key);
        if (baseItem == null) {
            return Optional.of(getFallbackInstance(stack));
        }
        
        // Create ItemInstance - it will automatically deserialize components from the ItemStack
        ItemInstance instance = new ItemInstance(baseItem, stack, serializationRegistry);

        return Optional.of(instance);
    }

    /**
     * Checks if the given ItemStack is of a specific BaseItem type.
     * @param stack The ItemStack to check
     * @param item The BaseItem to compare against
     * @return true if the ItemStack is of the specified BaseItem type, false otherwise
     */
    public boolean isItemOfType(@NotNull ItemStack stack, BaseItem item) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");
        Preconditions.checkNotNull(item, "BaseItem cannot be null");
        if (stack.getType() == Material.AIR) {
            return false; // No item to check
        }
        return fromItemStack(stack)
                .map(instance -> instance.getBaseItem().equals(item))
                .orElse(false);
    }

    private ItemInstance getFallbackInstance(@NotNull ItemStack stack) {
        final BaseItem fallbackItem = getFallbackItem(stack);
        return new ItemInstance(fallbackItem, stack, serializationRegistry);
    }

    /**
     * Gets the BaseItem for a given ItemStack.
     * @param stack The ItemStack to read
     * @return The BaseItem associated with the ItemStack, or a fallback item if not registered
     */
    public BaseItem getFallbackItem(@NotNull ItemStack stack) {
        final Material type = stack.getType();
        Preconditions.checkArgument(type.isItem(), "ItemStack cannot be air");
        // Fallback instance for items that are not registered
        BaseItem fallbackItem = itemRegistry.getFallbackItem(type);
        if (fallbackItem == null) {
            fallbackItem = new VanillaItem(type, ItemRarity.COMMON);
        }
        return fallbackItem;
    }

    /**
     * Gets the BaseItem for a given Material type.
     * @param type The Material type to read
     * @return The BaseItem associated with the Material type, or a fallback item if not registered
     */
    public BaseItem getFallbackItem(@NotNull Material type) {
        return getFallbackItem(new ItemStack(type));
    }

    /**
     * Gets all instances of {@link ItemInstance} from an array of ItemStacks.
     * @param stacks The array of ItemStacks to read
     * @return A set of ItemInstances created from the ItemStacks
     */
    @Contract(pure = true)
    public List<ItemInstance> fromArray(@Nullable ItemStack @NotNull [] stacks) {
        List<ItemInstance> instances = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null) {
                fromItemStack(stack).ifPresent(instances::add);
            }
        }
        return instances;
    }

    // Make a function that takes in an itemstack, converts it to an item instance, then gets the item stack from the item instance
    /**
     * Converts an ItemStack to an ItemInstance, then back to an ItemStack.
     * @param itemStack The ItemStack to convert
     * @return The ItemStack representation of the ItemInstance
     */
    @Contract(pure = true)
    public @NotNull Optional<ItemStack> convertItemStack(@NotNull ItemStack itemStack) {
        Preconditions.checkNotNull(itemStack, "ItemStack cannot be null");
        return fromItemStack(itemStack).map(ItemInstance::createItemStack);
    }

    /**
     * Checks if the BaseItem hashCode matches the value stored in the ItemStack's PersistentDataContainer.
     * @return true if up to date, false otherwise
     */
    public boolean isUpToDate(@NotNull ItemInstance instance) {
        Preconditions.checkNotNull(instance, "ItemInstance cannot be null");
        ItemStack stack = instance.createItemStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer hashCode = pdc.get(CoreNamespaceKeys.BASEITEM_HASHCODE_KEY, PersistentDataType.INTEGER);
        if (hashCode == null) {
            return false; // No hash code means it's not up to date
        }
        // todo: UPDATE ITEMS WHENEVER THEYRE CHECKED FOR VERSION
        return hashCode.equals(instance.getBaseItem().hashCode());
    }

    /**
     * Checks if the given ItemStack is a custom item.
     * @param stack The ItemStack to check
     * @return true if the ItemStack is a custom item, false otherwise
     */
    public boolean isCustomItem(@NotNull ItemStack stack) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");
        if (stack.getType() == Material.AIR) {
            return false; // No item to check
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false; // No metadata means it's not a custom item
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
    }
}