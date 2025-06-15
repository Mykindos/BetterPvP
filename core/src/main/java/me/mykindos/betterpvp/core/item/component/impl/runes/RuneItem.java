package me.mykindos.betterpvp.core.item.component.impl.runes;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for all rune items in the system.
 * Runes are special items that can be applied to equipment to enhance their properties.
 */
@Getter
public abstract class RuneItem extends BaseItem {

    private final Rune rune;

    /**
     * Creates a new rune with the specified properties
     *
     * @param model  The item model to use
     * @param rarity The rarity of this rune
     */
    protected RuneItem(Rune rune, ItemStack model, ItemRarity rarity) {
        super(rune.getName() + " Rune", model, ItemGroup.MATERIAL, rarity);
        this.rune = rune;
    }

}
