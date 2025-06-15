package me.mykindos.betterpvp.progression.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.bait.EventBaitItem;
import me.mykindos.betterpvp.progression.profession.fishing.bait.SpeedyBaitItem;
import me.mykindos.betterpvp.progression.profession.fishing.legendaries.Sharkbait;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
public class ProgressionItemBootstrap {

    private final Progression progression;
    private final ItemRegistry itemRegistry;

    @Inject
    private ProgressionItemBootstrap(Progression progression, ItemRegistry itemRegistry) {
        this.progression = progression;
        this.itemRegistry = itemRegistry;
    }

    @Inject
    private void registerBaits(SpeedyBaitItem speedyBait,
                               EventBaitItem eventBait) {
        itemRegistry.registerItem(new NamespacedKey(progression, "speedy_bait"), speedyBait);
        itemRegistry.registerItem(new NamespacedKey(progression, "event_bait"), eventBait);
    }

    @Inject
    private void registerWoodcutting() {
        // Register compacted log
        BaseItem compactedLog = createSimpleItem("Compacted Log", Material.OAK_WOOD, 1, true);
        BaseItem treeBark = createSimpleItem("Tree Bark", Material.GLISTERING_MELON_SLICE, 1, false);
        
        itemRegistry.registerItem(new NamespacedKey(progression, "compacted_log"), compactedLog);
        itemRegistry.registerItem(new NamespacedKey(progression, "tree_bark"), treeBark);
    }

    @Inject
    private void registerFishing(Sharkbait sharkbait) {
        itemRegistry.registerItem(new NamespacedKey(progression, "sharkbait"), sharkbait);
        
        // Register fish types
        registerFish("trout", "Trout", 1);
        registerFish("salmon", "Salmon", 2);
        registerFish("bluegill", "Bluegill", 3);
        registerFish("gar", "Gar", 4);
        registerFish("carp", "Carp", 5);
        registerFish("catfish", "Catfish", 6);
        registerFish("cod", "Cod", 7);
        registerFish("drum", "Drum", 8);
        registerFish("sablefish", "Sablefish", 9);
        registerFish("kingfish", "Kingfish", 10);
        registerFish("cobia", "Cobia", 11);
        registerFish("sea_bass", "Sea Bass", 12);
        registerFish("tuna", "Tuna", 13);
        registerFish("swordfish", "Swordfish", 14);
        registerFish("marlin", "Marlin", 15);
        registerFish("grouper", "Grouper", 16);
        registerFish("sturgeon", "Sturgeon", 17);
        registerFish("sunfish", "Sunfish", 18);
    }
    
    @Inject
    private void registerLegendaries(Sharkbait sharkbait) {
        itemRegistry.registerItem(new NamespacedKey(progression, "sharkbait"), sharkbait);
    }
    
    /**
     * Helper method to register a fish item
     * @param keyName The key name of the fish
     * @param modelData The model data value for the fish
     */
    private void registerFish(String keyName, String name, int modelData) {
        BaseItem fish = createSimpleItem(name, Material.COD, modelData, false);
        itemRegistry.registerItem(new NamespacedKey(progression, keyName), fish);
    }
    
    /**
     * Helper method to create a simple BaseItem
     * @param name The key name of the item
     * @param material The material of the item
     * @param modelData The model data value
     * @param glow Whether the item should glow
     * @return A BaseItem instance
     */
    private BaseItem createSimpleItem(String name, Material material, int modelData, boolean glow) {
        final ItemStack itemStack = ItemStack.of(material);
        itemStack.editMeta(meta -> {
            if (glow) {
                UtilItem.addGlow(meta);
            }
            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }
        });

        return new VanillaItem(name, itemStack, ItemRarity.COMMON);
    }
}
