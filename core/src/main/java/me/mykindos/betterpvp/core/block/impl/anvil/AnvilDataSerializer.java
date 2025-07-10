package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@CustomLog
public class AnvilDataSerializer implements SmartBlockDataSerializer<AnvilData> {

    private final StorageBlockDataSerializer<StorageBlockData> storageSerializer;
    private final ItemFactory itemFactory;
    private final AnvilRecipeRegistry anvilRecipeRegistry;

    public AnvilDataSerializer(ItemFactory itemFactory, AnvilRecipeRegistry anvilRecipeRegistry) {
        this.storageSerializer = new StorageBlockDataSerializer<>(StorageBlockData.class, itemFactory, StorageBlockData::new);
        this.itemFactory = itemFactory;
        this.anvilRecipeRegistry = anvilRecipeRegistry;
    }

    @Override
    public @NotNull Class<AnvilData> getType() {
        return AnvilData.class;
    }

    @Override
    public byte[] serializeToBytes(@NotNull AnvilData data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Serialize anvil items
            byte[] anvilItemsBytes = storageSerializer.serializeToBytes(data.getAnvilItems());
            dos.writeInt(anvilItemsBytes.length);
            dos.write(anvilItemsBytes);

            // Serialize primitive fields
            dos.writeInt(data.getHammerSwings());

            dos.flush();
            return baos.toByteArray();
        }
    }

    @Override
    public @NotNull AnvilData deserializeFromBytes(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {

            // Deserialize anvil items
            int anvilItemsLength = dis.readInt();
            byte[] anvilItemsBytes = new byte[anvilItemsLength];
            dis.readFully(anvilItemsBytes);
            StorageBlockData anvilItems = storageSerializer.deserializeFromBytes(anvilItemsBytes);

            // Deserialize primitive fields
            int hammerSwings = dis.readInt();

            // Create and populate AnvilData
            AnvilData anvilData = new AnvilData(itemFactory, anvilRecipeRegistry);
            anvilData.setAnvilItems(anvilItems);
            anvilData.setHammerSwings(hammerSwings);

            return anvilData;
        }
    }
} 