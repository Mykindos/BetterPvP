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
            Material.LIGHT,
            //</editor-fold>

            //<editor-fold desc="Portal / frame blocks">
            Material.END_PORTAL,
            Material.END_PORTAL_FRAME,
            Material.NETHER_PORTAL,
            //</editor-fold>

            //<editor-fold desc="Special cases not obtainable as items">
            Material.FARMLAND,
            Material.DIRT_PATH,
            Material.REINFORCED_DEEPSLATE,
            Material.BUDDING_AMETHYST,
            Material.CHORUS_PLANT,
            //</editor-fold>

            //<editor-fold desc="Infested blocks">
            Material.INFESTED_STONE,
            Material.INFESTED_COBBLESTONE,
            Material.INFESTED_STONE_BRICKS,
            Material.INFESTED_MOSSY_STONE_BRICKS,
            Material.INFESTED_CRACKED_STONE_BRICKS,
            Material.INFESTED_CHISELED_STONE_BRICKS,
            Material.INFESTED_DEEPSLATE,
            //</editor-fold>

            //<editor-fold desc="Entities / spawner">
            Material.SPAWNER,
            //</editor-fold>

            //<editor-fold desc="Special items">
            Material.DEBUG_STICK,
            Material.KNOWLEDGE_BOOK,
            //</editor-fold>

            //<editor-fold desc="Spawn eggs">
            Material.ALLAY_SPAWN_EGG,
            Material.ARMADILLO_SPAWN_EGG,
            Material.AXOLOTL_SPAWN_EGG,
            Material.BAT_SPAWN_EGG,
            Material.BEE_SPAWN_EGG,
            Material.BLAZE_SPAWN_EGG,
            Material.BREEZE_SPAWN_EGG,
            Material.BOGGED_SPAWN_EGG,
            Material.CAMEL_SPAWN_EGG,
            Material.CAT_SPAWN_EGG,
            Material.CAVE_SPIDER_SPAWN_EGG,
            Material.CHICKEN_SPAWN_EGG,
            Material.CREAKING_SPAWN_EGG,
            Material.COD_SPAWN_EGG,
            Material.COW_SPAWN_EGG,
            Material.CREEPER_SPAWN_EGG,
            Material.DOLPHIN_SPAWN_EGG,
            Material.DONKEY_SPAWN_EGG,
            Material.DROWNED_SPAWN_EGG,
            Material.ELDER_GUARDIAN_SPAWN_EGG,
            Material.ENDERMAN_SPAWN_EGG,
            Material.ENDERMITE_SPAWN_EGG,
            Material.ENDER_DRAGON_SPAWN_EGG,
            Material.EVOKER_SPAWN_EGG,
            Material.FOX_SPAWN_EGG,
            Material.FROG_SPAWN_EGG,
            Material.GHAST_SPAWN_EGG,
            Material.GLOW_SQUID_SPAWN_EGG,
            Material.GOAT_SPAWN_EGG,
            Material.GUARDIAN_SPAWN_EGG,
            Material.HAPPY_GHAST_SPAWN_EGG,
            Material.HOGLIN_SPAWN_EGG,
            Material.HORSE_SPAWN_EGG,
            Material.HUSK_SPAWN_EGG,
            Material.IRON_GOLEM_SPAWN_EGG,
            Material.LLAMA_SPAWN_EGG,
            Material.MAGMA_CUBE_SPAWN_EGG,
            Material.MOOSHROOM_SPAWN_EGG,
            Material.MULE_SPAWN_EGG,
            Material.OCELOT_SPAWN_EGG,
            Material.PANDA_SPAWN_EGG,
            Material.PARROT_SPAWN_EGG,
            Material.PHANTOM_SPAWN_EGG,
            Material.PIG_SPAWN_EGG,
            Material.PIGLIN_SPAWN_EGG,
            Material.PIGLIN_BRUTE_SPAWN_EGG,
            Material.PILLAGER_SPAWN_EGG,
            Material.POLAR_BEAR_SPAWN_EGG,
            Material.PUFFERFISH_SPAWN_EGG,
            Material.RABBIT_SPAWN_EGG,
            Material.RAVAGER_SPAWN_EGG,
            Material.SALMON_SPAWN_EGG,
            Material.SHEEP_SPAWN_EGG,
            Material.SHULKER_SPAWN_EGG,
            Material.SILVERFISH_SPAWN_EGG,
            Material.SKELETON_SPAWN_EGG,
            Material.SKELETON_HORSE_SPAWN_EGG,
            Material.SLIME_SPAWN_EGG,
            Material.SNIFFER_SPAWN_EGG,
            Material.SNOW_GOLEM_SPAWN_EGG,
            Material.SPIDER_SPAWN_EGG,
            Material.SQUID_SPAWN_EGG,
            Material.STRAY_SPAWN_EGG,
            Material.STRIDER_SPAWN_EGG,
            Material.TADPOLE_SPAWN_EGG,
            Material.TRADER_LLAMA_SPAWN_EGG,
            Material.TROPICAL_FISH_SPAWN_EGG,
            Material.TURTLE_SPAWN_EGG,
            Material.VEX_SPAWN_EGG,
            Material.VILLAGER_SPAWN_EGG,
            Material.VINDICATOR_SPAWN_EGG,
            Material.WANDERING_TRADER_SPAWN_EGG,
            Material.WARDEN_SPAWN_EGG,
            Material.WITCH_SPAWN_EGG,
            Material.WITHER_SKELETON_SPAWN_EGG,
            Material.WOLF_SPAWN_EGG,
            Material.ZOGLIN_SPAWN_EGG,
            Material.ZOMBIE_SPAWN_EGG,
            Material.ZOMBIE_HORSE_SPAWN_EGG,
            Material.ZOMBIE_VILLAGER_SPAWN_EGG,
            Material.ZOMBIFIED_PIGLIN_SPAWN_EGG
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
