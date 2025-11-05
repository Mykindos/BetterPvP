package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;

@Singleton
public class FishingRod extends VanillaItem {

    @Inject
    private FishingRod() {
        super(Material.FISHING_ROD, ItemRarity.COMMON);
        addSerializableComponent(new RuneContainerComponent(2));
    }

}
