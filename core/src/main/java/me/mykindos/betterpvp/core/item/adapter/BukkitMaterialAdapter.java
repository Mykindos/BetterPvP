package me.mykindos.betterpvp.core.item.adapter;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@CustomLog
public class BukkitMaterialAdapter {

    private static final List<Material> excludedMaterials = List.of(
            //<editor-fold desc="Air">
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            //</editor-fold>

            //<editor-fold desc="Technical / unobtainable blocks">
            Material.BARRIER,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.TEST_INSTANCE_BLOCK,
            Material.LIGHT
            //</editor-fold>
    );

    public static void registerDefaults(Map<Material, BaseItem> fallbackItems,
                                        Map<NamespacedKey, BaseItem> items,
                                        ConcurrentSkipListMap<NamespacedKey, BaseItem> sortedItems) {
        log.info("Registering default bukkit fallback items...").submit();
        long start = System.nanoTime();

        Arrays.stream(Material.values())
                .filter(Material::isItem)
                .filter(material -> !excludedMaterials.contains(material))
                .parallel()
                .forEach(material -> {
                    final VanillaItem item = new VanillaItem(material, ItemRarity.COMMON);
                    fallbackItems.put(material, item);
                    items.putIfAbsent(material.getKey(), item);
                    sortedItems.putIfAbsent(material.getKey(), item);
                });

        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;

        log.info("Registered {} default bukkit fallback items in {} ms",
                fallbackItems.size(), String.format("%.2f", ms)).submit();
    }
}
