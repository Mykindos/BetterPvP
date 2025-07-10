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
    private void registerOres() {
    }

    @Inject
    private void registerBlocks(Steel.BlockItem steelBlock) {
        registry.registerItem(key("steel_block"), steelBlock);
    }

    @Inject
    private void registerIngots(Steel.Ingot steelIngot) {
        registry.registerItem(key("steel_ingot"), steelIngot);
    }

}
