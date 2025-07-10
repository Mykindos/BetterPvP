package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.Alloy;
import me.mykindos.betterpvp.core.recipe.smelting.AlloyRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.LiquidAlloy;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingService;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@CustomLog
public class SmelterDataSerializer implements SmartBlockDataSerializer<SmelterData> {

    private final StorageBlockDataSerializer<StorageBlockData> storageSerializer;
    private final ItemFactory itemFactory;
    private final SmeltingService smeltingService;
    private final AlloyRegistry alloyRegistry;
    private final CastingMoldRecipeRegistry castingMoldRecipeRegistry;

    public SmelterDataSerializer(ItemFactory itemFactory, SmeltingService smeltingService, AlloyRegistry alloyRegistry, CastingMoldRecipeRegistry castingMoldRecipeRegistry) {
        this.storageSerializer = new StorageBlockDataSerializer<>(StorageBlockData.class, itemFactory, StorageBlockData::new);
        this.itemFactory = itemFactory;
        this.smeltingService = smeltingService;
        this.alloyRegistry = alloyRegistry;
        this.castingMoldRecipeRegistry = castingMoldRecipeRegistry;
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

            // Serialize result items
            byte[] resultBytes = storageSerializer.serializeToBytes(data.getResultItems());
            dos.writeInt(resultBytes.length);
            dos.write(resultBytes);

            // Serialize fuel items
            byte[] fuelBytes = storageSerializer.serializeToBytes(data.getFuelItems());
            dos.writeInt(fuelBytes.length);
            dos.write(fuelBytes);

            // Serialize primitive fields
            dos.writeLong(data.getMaxBurnTime());
            dos.writeInt(data.getMaxLiquidCapacity());
            dos.writeLong(data.getBurnTime());
            dos.writeFloat(data.getTemperature());

            // Serialize stored liquid alloy
            if (data.getStoredLiquid() != null) {
                dos.writeBoolean(true);
                dos.writeUTF(data.getStoredLiquid().getAlloyType().getName());
                dos.writeInt(data.getStoredLiquid().getMillibuckets());
            } else {
                dos.writeBoolean(false);
            }

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

            // Deserialize result items
            int resultLength = dis.readInt();
            byte[] resultBytes = new byte[resultLength];
            dis.readFully(resultBytes);
            StorageBlockData resultItems = storageSerializer.deserializeFromBytes(resultBytes);

            // Deserialize fuel items
            int fuelLength = dis.readInt();
            byte[] fuelBytes = new byte[fuelLength];
            dis.readFully(fuelBytes);
            StorageBlockData fuelItems = storageSerializer.deserializeFromBytes(fuelBytes);

            // Deserialize primitive fields
            long maxBurnTime = dis.readLong();
            int maxLiquidCapacity = dis.readInt();
            long burnTime = dis.readLong();
            float temperature = dis.readFloat();

            // Deserialize stored liquid alloy
            LiquidAlloy storedLiquid = null;
            if (dis.readBoolean()) {
                String alloyName = dis.readUTF();
                int millibuckets = dis.readInt();
                // Look up the alloy by name in the registry
                try {
                    Alloy alloy = alloyRegistry.getByName(alloyName);
                    storedLiquid = new LiquidAlloy(alloy, millibuckets);
                } catch (Exception e) {
                    log.warn("Failed to deserialize liquid alloy: {} with {} mB - {}", alloyName, millibuckets, e.getMessage()).submit();
                }
            }

            // Create and populate SmelterData
            SmelterData smelterData = new SmelterData(smeltingService, itemFactory, castingMoldRecipeRegistry, maxBurnTime, maxLiquidCapacity);
            smelterData.setContentItems(contentItems);
            smelterData.setResultItems(resultItems);
            smelterData.setFuelItems(fuelItems);
            smelterData.setBurnTime(burnTime);
            smelterData.setTemperature(temperature);
            smelterData.setStoredLiquid(storedLiquid);

            return smelterData;
        }
    }
}
