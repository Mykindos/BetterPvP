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

/**
 * Serializer for StorageBlockData.
 */
public class StorageBlockDataSerializer implements SmartBlockDataSerializer<StorageBlockData> {
    
    private static final NamespacedKey CONTENT_KEY = new NamespacedKey("betterpvp", "content");
    private final ItemFactory itemFactory;
    
    public StorageBlockDataSerializer(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }
    
    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("betterpvp", "storage");
    }
    
    @Override
    public @NotNull Class<StorageBlockData> getType() {
        return StorageBlockData.class;
    }
    
    @Override
    public void serialize(@NotNull StorageBlockData data, @NotNull PersistentDataContainer container) {
        List<ItemInstance> content = data.getContent();
        ItemStack[] itemStacks = content.stream()
                .map(ItemInstance::createItemStack)
                .toArray(ItemStack[]::new);
        container.set(CONTENT_KEY, DataType.ITEM_STACK_ARRAY, itemStacks);
    }
    
    @Override
    public @NotNull StorageBlockData deserialize(@NotNull PersistentDataContainer container) {
        ItemStack[] itemStacks = Objects.requireNonNullElse(
                container.get(CONTENT_KEY, DataType.ITEM_STACK_ARRAY), 
                new ItemStack[0]
        );
        List<ItemInstance> items = itemFactory.fromArray(itemStacks);
        return new StorageBlockData(items);
    }
    
    @Override
    public boolean hasData(@NotNull PersistentDataContainer container) {
        return container.has(CONTENT_KEY, DataType.ITEM_STACK_ARRAY);
    }
} 