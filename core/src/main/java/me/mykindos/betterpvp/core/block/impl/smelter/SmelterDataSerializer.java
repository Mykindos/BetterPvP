package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.Alloy;
import me.mykindos.betterpvp.core.recipe.smelting.AlloyRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.LiquidAlloy;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

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

            // Serialize content items through the processing engine
            byte[] contentBytes = storageSerializer.serializeToBytes(data.getProcessingEngine().getContentItems());
            dos.writeInt(contentBytes.length);
            dos.write(contentBytes);

            // Serialize result items through the processing engine
            byte[] resultBytes = storageSerializer.serializeToBytes(data.getProcessingEngine().getResultItems());
            dos.writeInt(resultBytes.length);
            dos.write(resultBytes);

            // Serialize fuel items through the fuel manager
            byte[] fuelBytes = storageSerializer.serializeToBytes(data.getFuelManager().getFuelItems());
            dos.writeInt(fuelBytes.length);
            dos.write(fuelBytes);

            // Serialize casting mold through the processing engine
            if (data.getProcessingEngine().getCastingMold() != null) {
                dos.writeBoolean(true);
                final NamespacedKey key = Objects.requireNonNull(itemFactory.getItemRegistry().getKey(data.getProcessingEngine().getCastingMold()),
                        "Casting mold item must have a valid key");
                dos.writeUTF(key.toString());
            } else {
                dos.writeBoolean(false);
            }

            byte[] castingMoldBytes = storageSerializer.serializeToBytes(data.getProcessingEngine().getCastingMoldItems());
            dos.writeInt(castingMoldBytes.length);
            dos.write(castingMoldBytes);

            // Serialize primitive fields
            dos.writeLong(data.getMaxBurnTime());
            dos.writeInt(data.getMaxLiquidCapacity());
            dos.writeLong(data.getFuelManager().getBurnTime());
            dos.writeFloat(data.getFuelManager().getTemperature());

            // Serialize stored liquid alloy through the liquid manager
            if (data.getLiquidManager().getStoredLiquid() != null) {
                dos.writeBoolean(true);
                dos.writeUTF(data.getLiquidManager().getStoredLiquid().getAlloyType().getName());
                dos.writeInt(data.getLiquidManager().getStoredLiquid().getMillibuckets());
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

            // Deserialize casting mold
            CastingMold castingMold = null;
            if (dis.readBoolean()) {
                String moldKey = dis.readUTF();
                final BaseItem found = itemFactory.getItemRegistry().getItem(moldKey);
                if (!(found instanceof CastingMold mold)) {
                    throw new IOException("Casting mold item must be an instance of CastingMold");
                }

                castingMold = mold;
            }
            int castingMoldLength = dis.readInt();
            byte[] castingMoldBytes = new byte[castingMoldLength];
            dis.readFully(castingMoldBytes);
            StorageBlockData castingMoldItems = storageSerializer.deserializeFromBytes(castingMoldBytes);

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

            // Create SmelterData with the new component structure
            SmelterData smelterData = new SmelterData(smeltingService, itemFactory, castingMoldRecipeRegistry, maxBurnTime, maxLiquidCapacity);
            
            // Set content items in the processing engine
            smelterData.getProcessingEngine().getContentItems().setContent(contentItems.getContent());
            
            // Set result items in the processing engine
            smelterData.getProcessingEngine().getResultItems().setContent(resultItems.getContent());
            
            // Set fuel items in the fuel manager
            smelterData.getFuelManager().getFuelItems().setContent(fuelItems.getContent());
            
            // Set fuel and temperature state in the fuel manager
            smelterData.getFuelManager().setBurnTime(burnTime);
            smelterData.getFuelManager().setTemperature(temperature);
            
            // Set liquid state in the liquid manager
            smelterData.getLiquidManager().setStoredLiquid(storedLiquid);
            
            // Set casting mold state in the processing engine
            smelterData.getProcessingEngine().getCastingMoldItems().setContent(castingMoldItems.getContent());
            smelterData.getProcessingEngine().setCastingMold(castingMold);
            
            return smelterData;
        }
    }
}
