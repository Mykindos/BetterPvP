package me.mykindos.betterpvp.core.block.data.storage;

import io.lumine.mythic.bukkit.utils.pdc.DataType;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Serializer for StorageBlockData.
 */
public final class StorageBlockDataSerializer<T extends StorageBlockData> implements SmartBlockDataSerializer<T> {
    
    private static final NamespacedKey CONTENT_KEY = new NamespacedKey("betterpvp", "content");
    private final String key;
    private final Class<T> dataType;
    private final ItemFactory itemFactory;
    private final Function<List<ItemInstance>, T> constructor;
    
    public StorageBlockDataSerializer(String key, Class<T> dataType, ItemFactory itemFactory, Function<List<ItemInstance>, T> constructor) {
        this.key = key;
        this.dataType = dataType;
        this.itemFactory = itemFactory;
        this.constructor = constructor;
    }

    @Override
    public @NotNull Class<T> getType() {
        return dataType;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("betterpvp", key);
    }
    
    @Override
    public void serialize(@NotNull T data, @NotNull PersistentDataContainer container) {
        List<ItemInstance> content = data.getContent();
        ItemStack[] itemStacks = content.stream()
                .map(ItemInstance::createItemStack)
                .toArray(ItemStack[]::new);
        container.set(CONTENT_KEY, DataType.ITEM_STACK_ARRAY, itemStacks);
    }
    
    @Override
    public @NotNull T deserialize(@NotNull PersistentDataContainer container) {
        ItemStack[] itemStacks = Objects.requireNonNullElse(
                container.get(CONTENT_KEY, DataType.ITEM_STACK_ARRAY), 
                new ItemStack[0]
        );
        List<ItemInstance> items = itemFactory.fromArray(itemStacks);
        return constructor.apply(items);
    }
    
    @Override
    public boolean hasData(@NotNull PersistentDataContainer container) {
        return container.has(CONTENT_KEY, DataType.ITEM_STACK_ARRAY);
    }
} 