package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.UnloadHandler;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Getter
@Setter
public class SmelterData implements RemovalHandler, UnloadHandler {

    private final long maxBurnTime;
    private StorageBlockData contentItems = new StorageBlockData(10);
    private StorageBlockData fuelItems = new StorageBlockData(1);
    private long burnTime = 0L; // Millis
    private float temperature = 0.0f; // Celsius

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        // Drop items from both nested inventories by delegating to their removal handlers
        contentItems.onRemoval(instance, cause);
        fuelItems.onRemoval(instance, cause);
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        if (contentItems instanceof UnloadHandler contentUnloadHandler) {
            contentUnloadHandler.onUnload(instance);
        }
        if (fuelItems instanceof UnloadHandler fuelUnloadHandler) {
            fuelUnloadHandler.onUnload(instance);
        }
    }
}
