package me.mykindos.betterpvp.core.metal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;

@Singleton
@PluginAdapter("Core")
public class MetalItemBootstrap {

    private final Core core;
    private final ItemRegistry registry;

    @Inject
    private MetalItemBootstrap(Core core, ItemRegistry registry) {
        this.core = core;
        this.registry = registry;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(core, name);
    }

    @Inject
    private void registerOres(FissureQuartz.DeepslateOreItem fissureQuartzDeepslateOre,
                              FissureQuartz.OreItem fissureQuartzOre,
                              Runesteel.OreBlockItem runebloodOre) {
        registry.registerItem(key("fissure_quartz_deepslate_ore"), fissureQuartzDeepslateOre);
        registry.registerItem(key("fissure_quartz_stone_ore"), fissureQuartzOre);
        registry.registerItem(key("runeblood_ore"), runebloodOre);
    }

    @Inject
    private void registerBlocks(Steel.BlockItem steelBlock,
                                Runesteel.BlockItem runesteelBlock) {
        registry.registerItem(key("steel_block"), steelBlock);
        registry.registerItem(key("runesteel_block"), runesteelBlock);
    }

    @Inject
    private void registerIngots(Steel.Ingot steelIngot,
                                Runesteel.Ingot runesteelIngot,
                                Runesteel.Fragment runebloodOre,
                                Runesteel.Billet runesteelBillet,
                                FissureQuartz.Item fissureQuartz) {
        registry.registerItem(key("steel_ingot"), steelIngot);
        registry.registerItem(key("runesteel_ingot"), runesteelIngot);
        registry.registerItem(key("runeblood_fragment"), runebloodOre);
        registry.registerItem(key("runesteel_billet"), runesteelBillet);
        registry.registerItem(key("fissure_quartz"), fissureQuartz);
    }

}
