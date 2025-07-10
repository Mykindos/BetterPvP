package me.mykindos.betterpvp.core.metal.casting;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;

public class CastingMold extends BaseItem {

    private static final ItemStack model;

    static {
        model = ItemStack.of(Material.PAPER);
        model.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "casting_mold/base"));
    }

    public CastingMold(String name, ItemStack model) {
        super(name, model, ItemGroup.MATERIAL, ItemRarity.COMMON);
    }

    public CastingMold(String name, @Subst("base") String model) {
        this(name, ItemView.builder()
                .material(Material.PAPER)
                .maxStackSize(1)
                .itemModel(Key.key("betterpvp", "casting_mold/" + model))
                .build().get());
    }
}
