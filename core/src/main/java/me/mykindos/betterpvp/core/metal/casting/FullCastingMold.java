package me.mykindos.betterpvp.core.metal.casting;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

@Getter
public class FullCastingMold extends BaseItem {

    private final BaseItem yield;
    private final CastingMold emptyMold;

    public FullCastingMold(ItemStack model, CastingMold emptyMold, BaseItem yield) {
        super("", model, ItemGroup.MATERIAL, ItemRarity.COMMON);
        this.yield = yield;
        this.emptyMold = emptyMold;
        setItemNameRenderer(emptyMold.getItemNameRenderer()); // adopt the empty mold's name renderer
        setInstanceRarityProvider(yield.getInstanceRarityProvider()); // adopt the yield's rarity provider
    }

    public FullCastingMold(String model, String variant, CastingMold emptyMold, BaseItem yield) {
        this(createModel(model, variant), emptyMold, yield);
    }

    private static ItemStack createModel(@Subst("base") @NotNull String model, @NotNull String variant) {
        ItemStack result = ItemStack.of(Material.PAPER);
        result.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "casting_mold/" + model));
        result.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(variant).build());
        result.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return result;
    }
}
