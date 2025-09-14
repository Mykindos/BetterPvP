package me.mykindos.betterpvp.progression.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
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
        BaseItem compactedLog = new BaseItem("Compacted Log", ItemView.builder().material(Material.OAK_WOOD).glow(true).build().get(), ItemGroup.BLOCK, ItemRarity.COMMON);
        BaseItem treeBark = new BaseItem("Tree Bark", Item.model("tree_bark", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);
        
        itemRegistry.registerItem(new NamespacedKey(progression, "compacted_log"), compactedLog);
        itemRegistry.registerItem(new NamespacedKey(progression, "tree_bark"), treeBark);
    }
    
    @Inject
    private void registerLegendaries(Sharkbait sharkbait) {
        itemRegistry.registerItem(new NamespacedKey(progression, "sharkbait"), sharkbait);
    }

}
