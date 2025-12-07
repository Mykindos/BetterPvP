package me.mykindos.betterpvp.core.block.impl.imbuement;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeRegistry;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@CustomLog
public class ImbuementPedestalDataSerializer implements SmartBlockDataSerializer<ImbuementPedestalData> {

    private final StorageBlockDataSerializer<StorageBlockData> storageSerializer;
    private final ItemFactory itemFactory;
    private final ImbuementRecipeRegistry imbuementRecipeRegistry;

    public ImbuementPedestalDataSerializer(ItemFactory itemFactory, ImbuementRecipeRegistry imbuementRecipeRegistry) {
        this.storageSerializer = new StorageBlockDataSerializer<>(StorageBlockData.class, itemFactory, StorageBlockData::new);
        this.itemFactory = itemFactory;
        this.imbuementRecipeRegistry = imbuementRecipeRegistry;
    }

    @Override
    public @NotNull Class<ImbuementPedestalData> getType() {
        return ImbuementPedestalData.class;
    }

    @Override
    public byte[] serializeToBytes(@NotNull ImbuementPedestalData data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Serialize pedestal items through the item manager
            byte[] pedestalItemsBytes = storageSerializer.serializeToBytes(data.getItemManager().getPedestalItems());
            dos.writeInt(pedestalItemsBytes.length);
            dos.write(pedestalItemsBytes);

            // Note: We don't serialize the flying items, recipe execution state, or display entities
            // as these are runtime state that should be recreated on load

            dos.flush();
            return baos.toByteArray();
        }
    }

    @Override
    public @NotNull ImbuementPedestalData deserializeFromBytes(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {

            // Deserialize pedestal items
            int pedestalItemsLength = dis.readInt();
            byte[] pedestalItemsBytes = new byte[pedestalItemsLength];
            dis.readFully(pedestalItemsBytes);
            StorageBlockData pedestalItems = storageSerializer.deserializeFromBytes(pedestalItemsBytes);

            // Create ImbuementPedestalData with the new component structure
            ImbuementPedestalData pedestalData = new ImbuementPedestalData(itemFactory, imbuementRecipeRegistry);
            
            // Set the pedestal items in the item manager
            pedestalData.getItemManager().getPedestalItems().setContent(pedestalItems.getContent());

            return pedestalData;
        }
    }
} 