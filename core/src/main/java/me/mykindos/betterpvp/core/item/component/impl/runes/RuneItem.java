package me.mykindos.betterpvp.core.item.component.impl.runes;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;

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
    protected RuneItem(Rune rune, RuneColor color, ItemRarity rarity) {
        this("Rune of " + rune.getName(), color, rune, rarity);
    }

    protected RuneItem(String name, RuneColor color, Rune rune, ItemRarity rarity) {
        super(name, Item.builder("rune").customModelData(color.getModelData()).build(), ItemGroup.MATERIAL, rarity);
        this.rune = rune;
        addBaseComponent(new RuneDescriptionComponent(rune));
    }

}
