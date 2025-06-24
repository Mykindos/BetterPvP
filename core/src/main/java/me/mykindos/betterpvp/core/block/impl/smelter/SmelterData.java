package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.data.storage.StorageBlockData;

@RequiredArgsConstructor
@Getter
@Setter
public class SmelterData {

    private final long maxBurnTime;
    private StorageBlockData contentItems = new StorageBlockData(10);
    private StorageBlockData fuelItems = new StorageBlockData(1);
    private long burnTime = 0L; // Millis
    private float temperature = 0.0f; // Celsius

}
