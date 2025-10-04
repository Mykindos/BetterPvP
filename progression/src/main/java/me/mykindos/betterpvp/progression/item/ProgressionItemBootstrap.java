package me.mykindos.betterpvp.progression.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.bait.EventBaitItem;
import me.mykindos.betterpvp.progression.profession.fishing.bait.SpeedyBaitItem;
import me.mykindos.betterpvp.progression.profession.fishing.legendaries.Sharkbait;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
public class ProgressionItemBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private Progression progression;
    @Inject private SpeedyBaitItem speedyBait;
    @Inject private EventBaitItem eventBait;
    @Inject private Sharkbait sharkbait;

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        // Baits
        itemRegistry.registerItem(new NamespacedKey(progression, "speedy_bait"), speedyBait);
        itemRegistry.registerItem(new NamespacedKey(progression, "event_bait"), eventBait);

        // Woodcutting
        BaseItem compactedLog = new BaseItem("Compacted Log", ItemView.builder().material(Material.OAK_WOOD).glow(true).build().get(), ItemGroup.BLOCK, ItemRarity.COMMON);
        BaseItem treeBark = new BaseItem("Tree Bark", Item.model("tree_bark", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);

        itemRegistry.registerItem(new NamespacedKey(progression, "compacted_log"), compactedLog);
        itemRegistry.registerItem(new NamespacedKey(progression, "tree_bark"), treeBark);

        // Legendaries
        itemRegistry.registerItem(new NamespacedKey(progression, "sharkbait"), sharkbait);
    }

}
