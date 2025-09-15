package me.mykindos.betterpvp.progression.item;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
public class FishItem extends BaseItem implements NexoItem {

    private final String nexoId;

    public FishItem(String name, String nexoId) {
        super(name, ItemStack.of(Material.PAPER), ItemGroup.MATERIAL, ItemRarity.COMMON);
        this.nexoId = nexoId;
    }

    @Override
    public @NotNull String getId() {
        return nexoId;
    }
}
