package me.mykindos.betterpvp.core.block.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.impl.workbench.Workbench;

@Singleton
public class CoreBlockBootstrap {

    private final SmartBlockRegistry smartBlockRegistry;

    @Inject
    private CoreBlockBootstrap(SmartBlockRegistry smartBlockRegistry) {
        this.smartBlockRegistry = smartBlockRegistry;
    }

    @Inject
    private void registerBlocks(Workbench workbench) {
        smartBlockRegistry.registerBlock(workbench);
    }

}
