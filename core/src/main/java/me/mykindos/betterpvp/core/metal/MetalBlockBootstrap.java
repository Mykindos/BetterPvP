package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;

@Singleton
@PluginAdapter("Core")
public class MetalBlockBootstrap {

    private final SmartBlockRegistry registry;

    @Inject
    private MetalBlockBootstrap(SmartBlockRegistry registry) {
        this.registry = registry;
    }

    @Inject
    private void registerOres() {
    }

    @Inject
    private void registerBlocks(Steel.Block steelBlock) {
        registry.registerBlock(steelBlock);
    }



}
