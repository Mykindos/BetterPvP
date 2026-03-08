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
@ItemKey("core:force_powder")
public class ForcePowder extends BaseItem {

    @Inject
    public ForcePowder() {
        super("Force Powder", Item.model("force_powder", 64), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
        addBaseComponent(new StatAugmentationComponent(List.of(
                new StatAugmentation(StatTypes.MELEE_DAMAGE, 1, 0.01, StatAugmentation.Operation.ADD)
        )));
    }
}
