package me.mykindos.betterpvp.core.block.data.impl;

import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.function.Function;

/**
 * Serializer for StorageBlockData with optimized byte serialization.
 */
public final class StorageBlockDataSerializer<T extends StorageBlockData> implements SmartBlockDataSerializer<T> {
    
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
    public byte[] serializeToBytes(@NotNull T data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final DataOutputStream oos = new DataOutputStream(baos)) {

            // Write the max size first
            oos.writeInt(data.maxSize());

            // Get content and write the count
            final List<ItemStack> content = data.getContent().stream().map(ItemInstance::createItemStack).toList();
            oos.writeInt(content.size());

            // Write byte size
            final byte[] bytes = ItemStack.serializeItemsAsBytes(content);
            oos.writeInt(bytes.length);

            // Write ItemStacks
            oos.write(bytes);

            oos.flush();
            return baos.toByteArray();
        }
    }

    @Override
    public @NotNull T deserializeFromBytes(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                final DataInputStream ois = new DataInputStream(bais)) {

            // Read the max size
            int maxSize = ois.readInt();

            // Read the content size
            int contentSize = ois.readInt();

            // Read the byte size
            int byteSize = ois.readInt();

            // Read the ItemStacks bytes
            byte[] itemBytes = new byte[byteSize];
            ois.readFully(itemBytes);

            // Deserialize ItemStacks
            final @NotNull ItemStack[] itemStacks = ItemStack.deserializeItemsFromBytes(itemBytes);
            final List<ItemInstance> itemInstances = itemFactory.fromArray(itemStacks);
            if (itemInstances.size() > maxSize) {
                throw new IOException("Deserialized item count exceeds max size: " + itemInstances.size() + " > " + maxSize);
            }

            if (contentSize != itemInstances.size()) {
                throw new IOException("Deserialized item count does not match expected size: " + contentSize + " != " + itemInstances.size());
            }

            // Create the StorageBlockData instance
            return constructor.apply(itemInstances);
        }
    }
} 