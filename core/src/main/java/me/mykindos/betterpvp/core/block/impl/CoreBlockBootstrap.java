package me.mykindos.betterpvp.core.block.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.impl.forge.Forge;
import me.mykindos.betterpvp.core.block.impl.forge.ForgeItem;
import me.mykindos.betterpvp.core.block.impl.workbench.Workbench;
import me.mykindos.betterpvp.core.block.impl.workbench.WorkbenchItem;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;

@Singleton
public class CoreBlockBootstrap {

    private final Core core;
    private final SmartBlockRegistry smartBlockRegistry;
    private final ItemRegistry itemRegistry;

    @Inject
    private CoreBlockBootstrap(Core core, SmartBlockRegistry smartBlockRegistry, ItemRegistry itemRegistry) {
        this.core = core;
        this.smartBlockRegistry = smartBlockRegistry;
        this.itemRegistry = itemRegistry;
    }

    @Inject
    private void registerStations(Workbench workbench, WorkbenchItem workbenchItem,
                                  Forge forge, ForgeItem forgeItem) {
        smartBlockRegistry.registerBlock(workbench);
        itemRegistry.registerItem(new NamespacedKey(core, "workbench"), workbenchItem);

        smartBlockRegistry.registerBlock(forge);
        itemRegistry.registerItem(new NamespacedKey(core, "forge"), forgeItem);
    }

}
