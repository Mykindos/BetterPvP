package me.mykindos.betterpvp.progression.profession.woodcutting.item;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Singleton
@ItemKey("progression:tree_bark")
public class TreeBark extends BaseItem {

    public TreeBark() {
        super(translatableName("progression.item.tree-bark.name"), Item.model("tree_bark", 64), ItemGroup.MATERIAL, ItemRarity.COMMON);
        addBaseComponent(DescriptionComponent.translatable(1, "progression.item.tree-bark.lore",
                Component.text("Lumberjack", NamedTextColor.AQUA)));
    }
}
