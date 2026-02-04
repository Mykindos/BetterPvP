package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
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
    private final ClientManager clientManager;

    public AnvilDataSerializer(ItemFactory itemFactory, AnvilRecipeRegistry anvilRecipeRegistry, ClientManager clientManager) {
        this.storageSerializer = new StorageBlockDataSerializer<>(StorageBlockData.class, itemFactory, StorageBlockData::new);
        this.itemFactory = itemFactory;
        this.anvilRecipeRegistry = anvilRecipeRegistry;
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull Class<AnvilData> getType() {
        return AnvilData.class;
    }

    @Override
    public byte[] serializeToBytes(@NotNull AnvilData data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Serialize anvil items through the item manager
            byte[] anvilItemsBytes = storageSerializer.serializeToBytes(data.getItemManager().getAnvilItems());
            dos.writeInt(anvilItemsBytes.length);
            dos.write(anvilItemsBytes);

            // Serialize hammer swings through the hammer executor
            dos.writeInt(data.getHammerExecutor().getHammerSwings());

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

            // Deserialize hammer swings
            int hammerSwings = dis.readInt();

            // Create AnvilData with the new component structure
            AnvilData anvilData = new AnvilData(itemFactory, anvilRecipeRegistry, clientManager);
            
            // Set the anvil items in the item manager
            anvilData.getItemManager().getAnvilItems().setContent(anvilItems.getContent());
            
            // Set hammer swings in the hammer executor
            anvilData.getHammerExecutor().setHammerSwings(hammerSwings);

            return anvilData;
        }
    }
} 