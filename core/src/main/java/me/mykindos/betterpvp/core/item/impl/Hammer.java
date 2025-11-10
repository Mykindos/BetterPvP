package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
@ItemKey("core:hammer")
public class Hammer extends BaseItem implements NexoItem {

    @Inject
    private Hammer() {
        super("Hammer", ItemStack.of(Material.PAPER), ItemGroup.TOOL, ItemRarity.COMMON);
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_hammer_usable";
    }
}
