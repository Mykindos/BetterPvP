package me.mykindos.betterpvp.core.metal;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a metal ingot item in the game.
 * This class serves as a base for all metal ingots, providing common properties and methods.
 */
public abstract class MetalItem extends BaseItem implements NexoItem {

    private final String nexoId;

    protected MetalItem(String name, String nexoId, ItemRarity rarity) {
        super(name, ItemStack.of(Material.PAPER), ItemGroup.MATERIAL, rarity);
        this.nexoId = nexoId;
    }

    @Override
    public @NotNull String getId() {
        return nexoId;
    }
}
