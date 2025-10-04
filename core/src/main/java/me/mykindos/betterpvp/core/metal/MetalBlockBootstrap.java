package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemRegistry;

@Singleton
public class MetalBlockBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private SmartBlockRegistry registry;
    @Inject private FissureQuartz.DeepslateOre fissureQuartzDeepslateOre;
    @Inject private FissureQuartz.Ore fissureQuartzOre;
    @Inject private Runesteel.OreBlock runebloodOre;
    @Inject private Steel.Block steelBlock;
    @Inject private Runesteel.Block runesteelBlock;

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        // Ores
        registry.registerBlock(fissureQuartzDeepslateOre);
        registry.registerBlock(fissureQuartzOre);
        registry.registerBlock(runebloodOre);

        // Blocks
        registry.registerBlock(steelBlock);
        registry.registerBlock(runesteelBlock);
    }
}
