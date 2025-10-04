package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;

@Singleton
public class MetalItemBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private Core core;
    @Inject private FissureQuartz.DeepslateOreItem fissureQuartzDeepslateOre;
    @Inject private FissureQuartz.OreItem fissureQuartzOre;
    @Inject private Runesteel.OreBlockItem runebloodOre;
    @Inject private Steel.BlockItem steelBlock;
    @Inject private Runesteel.BlockItem runesteelBlock;
    @Inject private Steel.Ingot steelIngot;
    @Inject private Runesteel.Ingot runesteelIngot;
    @Inject private Runesteel.Fragment runebloodFragment;
    @Inject private Runesteel.Billet runesteelBillet;
    @Inject private FissureQuartz.Item fissureQuartz;

    private NamespacedKey key(String name) {
        return new NamespacedKey(core, name);
    }

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        // Ores
        itemRegistry.registerItem(key("fissure_quartz_deepslate_ore"), fissureQuartzDeepslateOre);
        itemRegistry.registerItem(key("fissure_quartz_stone_ore"), fissureQuartzOre);
        itemRegistry.registerItem(key("runeblood_ore"), runebloodOre);

        // Blocks
        itemRegistry.registerItem(key("steel_block"), steelBlock);
        itemRegistry.registerItem(key("runesteel_block"), runesteelBlock);

        // Ingots
        itemRegistry.registerItem(key("steel_ingot"), steelIngot);
        itemRegistry.registerItem(key("runesteel_ingot"), runesteelIngot);
        itemRegistry.registerItem(key("runeblood_fragment"), runebloodFragment);
        itemRegistry.registerItem(key("runesteel_billet"), runesteelBillet);
        itemRegistry.registerItem(key("fissure_quartz"), fissureQuartz);
    }

}
