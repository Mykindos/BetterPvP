package me.mykindos.betterpvp.core.metal;

import lombok.Getter;
import me.mykindos.betterpvp.core.block.custom.PlacesBlock;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * The item of a metal block, drawn with the block's item model and placing the block.
 */
@Getter
public abstract class MetalBlockItem extends BaseItem implements PlacesBlock {

    private final @NotNull String blockId;

    protected MetalBlockItem(Component name, @NotNull String blockId, ItemRarity rarity) {
        super(name, Item.builder(Material.PAPER).model("block/" + blockId).build(), ItemGroup.BLOCK, rarity);
        this.blockId = blockId;
    }

}
