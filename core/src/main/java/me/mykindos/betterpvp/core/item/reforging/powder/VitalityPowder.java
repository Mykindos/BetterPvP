package me.mykindos.betterpvp.core.item.reforging.powder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatAugmentation;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatAugmentationComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;

import java.util.List;

@Singleton
@ItemKey("core:vitality_powder")
public class VitalityPowder extends BaseItem {

    @Inject
    public VitalityPowder() {
        super("Vitality Powder", Item.model("vitality_powder", 64), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
        addBaseComponent(new StatAugmentationComponent(List.of(
                new StatAugmentation(StatTypes.HEALTH, 1, 0.01, StatAugmentation.Operation.ADD)
        )));
    }
}
