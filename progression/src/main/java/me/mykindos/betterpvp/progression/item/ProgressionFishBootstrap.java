package me.mykindos.betterpvp.progression.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
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
@PluginAdapter("Nexo")
public class ProgressionFishBootstrap {

    private final Progression progression;
    private final ItemRegistry itemRegistry;

    @Inject
    private ProgressionFishBootstrap(Progression progression, ItemRegistry itemRegistry) {
        this.progression = progression;
        this.itemRegistry = itemRegistry;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(progression, name);
    }

    @Inject
    private void registerFishing(Sharkbait sharkbait) {
        itemRegistry.registerItem(new NamespacedKey(progression, "sharkbait"), sharkbait);

        // Register fish types
        itemRegistry.registerItem(key("trout"), new FishItem("Trout", "trout"));
        itemRegistry.registerItem(key("salmon"), new FishItem("Salmon", "salmon"));
        itemRegistry.registerItem(key("bluegill"), new FishItem("Bluegill", "bluegill"));
        itemRegistry.registerItem(key("gar"), new FishItem("Gar", "gar"));
        itemRegistry.registerItem(key("carp"), new FishItem("Carp", "carp"));
        itemRegistry.registerItem(key("catfish"), new FishItem("Catfish", "catfish"));
        itemRegistry.registerFallbackItem(key("cod"), Material.COD, new FishItem("Cod", "acod"));
        itemRegistry.registerItem(key("drum"), new FishItem("Drum", "drum"));
        itemRegistry.registerItem(key("sablefish"), new FishItem("Sablefish", "sablefish"));
        itemRegistry.registerItem(key("kingfish"), new FishItem("Kingfish", "kingfish"));
        itemRegistry.registerItem(key("cobia"), new FishItem("Cobia", "cobia"));
        itemRegistry.registerItem(key("sea_bass"), new FishItem("Sea Bass", "sea_bass"));
        itemRegistry.registerItem(key("tuna"), new FishItem("Tuna", "tuna"));
        itemRegistry.registerItem(key("swordfish"), new FishItem("Swordfish", "swordfish"));
        itemRegistry.registerItem(key("marlin"), new FishItem("Marlin", "marlin"));
        itemRegistry.registerItem(key("grouper"), new FishItem("Grouper", "grouper"));
        itemRegistry.registerItem(key("sturgeon"), new FishItem("Sturgeon", "sturgeon"));
        itemRegistry.registerItem(key("sunfish"), new FishItem("Sunfish", "sunfish"));
    }

}
