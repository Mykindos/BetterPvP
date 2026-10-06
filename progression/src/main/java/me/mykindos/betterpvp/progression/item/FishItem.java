package me.mykindos.betterpvp.progression.item;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;

@EqualsAndHashCode(callSuper = true)
public class FishItem extends BaseItem {

    /**
     * @param model the item model under betterpvp:item/fishing/
     */
    public FishItem(String nameKey, String model) {
        super(translatableName(nameKey), Item.builder(Material.PAPER).model("item/fishing/" + model).build(), ItemGroup.MATERIAL, ItemRarity.COMMON);
    }

}
