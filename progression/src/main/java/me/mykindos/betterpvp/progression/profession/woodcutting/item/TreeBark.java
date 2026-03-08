package me.mykindos.betterpvp.progression.profession.woodcutting.item;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;

@Singleton
@ItemKey("progression:tree_bark")
public class TreeBark extends BaseItem {

    public TreeBark() {
        super("Tree Bark", Item.model("tree_bark", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);
    }
}
