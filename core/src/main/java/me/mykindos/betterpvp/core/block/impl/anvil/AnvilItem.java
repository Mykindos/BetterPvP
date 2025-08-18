package me.mykindos.betterpvp.core.block.impl.anvil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.nexo.NexoItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
public class AnvilItem extends BaseItem implements NexoItem {

    private static final ItemStack model;

    static {
        model = ItemStack.of(Material.PAPER);
        model.editMeta(meta -> meta.setMaxStackSize(1));
    }

    @Inject
    private AnvilItem() {
        super("Anvil", model, ItemGroup.BLOCK, ItemRarity.COMMON);
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_anvil";
    }

    @Override
    public boolean isFurniture() {
        return true;
    }
} 