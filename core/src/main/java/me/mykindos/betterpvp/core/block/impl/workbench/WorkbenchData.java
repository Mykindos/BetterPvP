package me.mykindos.betterpvp.core.block.impl.workbench;

import me.mykindos.betterpvp.core.block.data.storage.StorageBlockData;
import me.mykindos.betterpvp.core.item.ItemInstance;

import java.util.List;

public class WorkbenchData extends StorageBlockData {
    WorkbenchData(List<ItemInstance> content) {
        super(content);
    }

    WorkbenchData() {
        super();
    }
}
