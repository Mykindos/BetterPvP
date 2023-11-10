package me.mykindos.betterpvp.core.menu.button;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

@RequiredArgsConstructor
@AllArgsConstructor
public class BackButton extends ControlItem<Gui> {

    private final Windowed previousMenu;
    private Runnable onBack;

    @Override
    public ItemProvider getItemProvider(Gui gui) {
        if (previousMenu == null) {
            return gui.getBackground();
        }

        return ItemView.builder()
                .material(Material.PAPER)
                .customModelData(10003)
                .fallbackMaterial(Material.ARROW)
                .displayName(Component.text("Back", NamedTextColor.RED))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (previousMenu == null) {
            return;
        }

        previousMenu.show(player);
        if (onBack != null) {
            onBack.run();
        }
    }
}
