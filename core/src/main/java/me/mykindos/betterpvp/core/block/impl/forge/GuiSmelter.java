package me.mykindos.betterpvp.core.block.impl.forge;

import com.google.common.base.Preconditions;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import lombok.CustomLog;
import lombok.NonNull;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiSmelter extends AbstractGui implements Windowed {

    private final Smelter smelter;
    private final SmartBlockInstance blockInstance;

    public GuiSmelter(ItemFactory itemFactory, SmartBlockInstance blockInstance) {
        super(9, 6);
        Preconditions.checkState(blockInstance.getSmartBlock() instanceof Smelter,
                "The block instance must be of type Smelter, but was: " + blockInstance.getSmartBlock().getKey());

        this.blockInstance = blockInstance;
        this.smelter = (Smelter) blockInstance.getSmartBlock();

        final ItemStack first = ItemStack.of(Material.PAPER);
        first.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "menu/smelter/fuel_bar_generic"));
//        first.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "invisible"));
        first.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("0").build());

        final ItemStack second = ItemStack.of(Material.PAPER);
        second.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "menu/smelter/fuel_bar_generic"));
//        second.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "invisible"));
        second.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("4").build());

        final ItemStack third = ItemStack.of(Material.PAPER);
        third.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "menu/smelter/fuel_bar_generic"));
//        third.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "invisible"));
        third.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("8").build());

        final ItemStack fourth = ItemStack.of(Material.PAPER);
        fourth.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "menu/smelter/fuel_bar_generic"));
        fourth.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("12").build());

        applyStructure(new Structure(
                "0000000A0",
                "0000000B0",
                "0000000C0",
                "000X000D0",
                "000000000",
                "000000000")
                .addIngredient('X', ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/smelter/gold_output"))
                        .build())
                .addIngredient('A', first)
                .addIngredient('B', second)
                .addIngredient('C', third)
                .addIngredient('D', fourth)
        );
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_smelter>").font(NEXO);
    }
}