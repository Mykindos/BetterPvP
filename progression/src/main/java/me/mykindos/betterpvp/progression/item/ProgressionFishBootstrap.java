package me.mykindos.betterpvp.progression.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
public class ProgressionFishBootstrap {

    private final ItemRegistry itemRegistry;
    private final Progression progression;

    @Inject
    public ProgressionFishBootstrap(ItemRegistry itemRegistry, Progression progression) {
        this.itemRegistry = itemRegistry;
        this.progression = progression;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(progression, name);
    }

    public void register() {;
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
