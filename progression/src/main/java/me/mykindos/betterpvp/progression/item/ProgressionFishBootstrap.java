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
        itemRegistry.registerItem(key("trout"), new FishItem("progression.item.trout.name", "trout"));
        itemRegistry.registerItem(key("salmon"), new FishItem("progression.item.salmon.name", "salmon"));
        itemRegistry.registerItem(key("bluegill"), new FishItem("progression.item.bluegill.name", "bluegill"));
        itemRegistry.registerItem(key("gar"), new FishItem("progression.item.gar.name", "gar"));
        itemRegistry.registerItem(key("carp"), new FishItem("progression.item.carp.name", "carp"));
        itemRegistry.registerItem(key("catfish"), new FishItem("progression.item.catfish.name", "catfish"));
        itemRegistry.registerFallbackItem(key("cod"), Material.COD, new FishItem("progression.item.cod.name", "acod"));
        itemRegistry.registerItem(key("drum"), new FishItem("progression.item.drum.name", "drum"));
        itemRegistry.registerItem(key("sablefish"), new FishItem("progression.item.sablefish.name", "sablefish"));
        itemRegistry.registerItem(key("kingfish"), new FishItem("progression.item.kingfish.name", "kingfish"));
        itemRegistry.registerItem(key("cobia"), new FishItem("progression.item.cobia.name", "cobia"));
        itemRegistry.registerItem(key("sea_bass"), new FishItem("progression.item.sea-bass.name", "sea_bass"));
        itemRegistry.registerItem(key("tuna"), new FishItem("progression.item.tuna.name", "tuna"));
        itemRegistry.registerItem(key("swordfish"), new FishItem("progression.item.swordfish.name", "swordfish"));
        itemRegistry.registerItem(key("marlin"), new FishItem("progression.item.marlin.name", "marlin"));
        itemRegistry.registerItem(key("grouper"), new FishItem("progression.item.grouper.name", "grouper"));
        itemRegistry.registerItem(key("sturgeon"), new FishItem("progression.item.sturgeon.name", "sturgeon"));
        itemRegistry.registerItem(key("sunfish"), new FishItem("progression.item.sunfish.name", "sunfish"));
    }
}
