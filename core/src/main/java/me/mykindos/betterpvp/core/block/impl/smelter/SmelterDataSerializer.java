package me.mykindos.betterpvp.core.block.impl.smelter;

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

public class SmelterDataSerializer implements SmartBlockDataSerializer<SmelterData> {

    private final StorageBlockDataSerializer<StorageBlockData> storageSerializer;

    public SmelterDataSerializer(ItemFactory itemFactory) {
        this.storageSerializer = new StorageBlockDataSerializer<>(StorageBlockData.class, itemFactory, StorageBlockData::new);
    }

    @Override
    public @NotNull Class<SmelterData> getType() {
        return SmelterData.class;
    }

    @Override
    public byte[] serializeToBytes(@NotNull SmelterData data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Serialize content items
            byte[] contentBytes = storageSerializer.serializeToBytes(data.getContentItems());
            dos.writeInt(contentBytes.length);
            dos.write(contentBytes);

            // Serialize fuel items
            byte[] fuelBytes = storageSerializer.serializeToBytes(data.getFuelItems());
            dos.writeInt(fuelBytes.length);
            dos.write(fuelBytes);

            // Serialize primitive fields
            dos.writeLong(data.getMaxBurnTime());
            dos.writeLong(data.getBurnTime());
            dos.writeFloat(data.getTemperature());

            dos.flush();
            return baos.toByteArray();
        }
    }

    @Override
    public @NotNull SmelterData deserializeFromBytes(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {

            // Deserialize content items
            int contentLength = dis.readInt();
            byte[] contentBytes = new byte[contentLength];
            dis.readFully(contentBytes);
            StorageBlockData contentItems = storageSerializer.deserializeFromBytes(contentBytes);

            // Deserialize fuel items
            int fuelLength = dis.readInt();
            byte[] fuelBytes = new byte[fuelLength];
            dis.readFully(fuelBytes);
            StorageBlockData fuelItems = storageSerializer.deserializeFromBytes(fuelBytes);

            // Deserialize primitive fields
            long maxBurnTime = dis.readLong();
            long burnTime = dis.readLong();
            float temperature = dis.readFloat();

            // Create and populate SmelterData
            SmelterData smelterData = new SmelterData(maxBurnTime);
            smelterData.setContentItems(contentItems);
            smelterData.setFuelItems(fuelItems);
            smelterData.setBurnTime(burnTime);
            smelterData.setTemperature(temperature);

            return smelterData;
        }
    }
}
