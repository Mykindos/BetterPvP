package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

/**
 * Rough metal-bearing rock, and the only thing the training mine yields.
 * <p>
 * A "bloom" is the spongy mass of metal a primitive furnace pulls out of ore — the first solid a young settlement can
 * actually make something from, which is precisely this item's job: it is not gear, it is the promise of gear, and it
 * buys a starter set from the quartermaster and nothing else.
 * <p>
 * A custom item rather than coal or raw copper on purpose. A vanilla material would carry vanilla recipes and furnace
 * uses, and the mine's output would leak into the wider economy through paths nobody chose. This has exactly one use,
 * so the mine can never pay out through a route it was not designed for.
 */
@Singleton
@ItemKey("core:bloomstone")
public class Bloomstone extends BaseItem {

    @Inject
    private Bloomstone() {
        super(translatableName("core.item.bloomstone.name"), Item.model("bloomstone", 64),
                ItemGroup.MATERIAL, ItemRarity.COMMON);
    }
}
