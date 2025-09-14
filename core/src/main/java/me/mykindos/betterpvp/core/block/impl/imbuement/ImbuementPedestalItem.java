package me.mykindos.betterpvp.core.block.impl.imbuement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ImbuementPedestalItem extends BaseItem implements NexoItem {

    private static final ItemStack model;

    static {
        model = ItemStack.of(Material.PAPER);
        model.editMeta(meta -> meta.setMaxStackSize(1));
    }

    @Inject
    private ImbuementPedestalItem() {
        super("Imbuement Pedestal", model, ItemGroup.BLOCK, ItemRarity.UNCOMMON);
    }

    @Override
    public @NotNull String getId() {
        return "imbuement_pedestal";
    }

    @Override
    public boolean isFurniture() {
        return true;
    }
} 