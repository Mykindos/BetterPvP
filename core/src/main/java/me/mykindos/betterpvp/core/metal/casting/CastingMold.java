package me.mykindos.betterpvp.core.metal.casting;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;

public abstract class CastingMold extends BaseItem {

    protected CastingMold(String name, ItemStack model) {
        super(name, model, ItemGroup.MATERIAL, ItemRarity.COMMON);
    }

    protected CastingMold(String name, @Subst("base") String model) {
        this(name, ItemView.builder()
                .material(Material.PAPER)
                .maxStackSize(1)
                .itemModel(Key.key("betterpvp", "item/casting_mold/" + model))
                .build().get());
    }
}
