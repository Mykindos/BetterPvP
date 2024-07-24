package me.mykindos.betterpvp.core.menu.button;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@AllArgsConstructor
public class BackButton extends ControlItem<Gui> {

    private final Windowed previousMenu;
    private Runnable onBack;

    @Override
    public ItemProvider getItemProvider(Gui gui) {
        return ItemView.builder()
                .material(Material.PAPER)
                .customModelData(10003)
                .fallbackMaterial(Material.ARROW)
                .displayName(Component.text(previousMenu == null ? "Close" : "Back", NamedTextColor.RED))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (previousMenu == null) {
            player.closeInventory();
            return;
        }

        previousMenu.show(player);
        if (onBack != null) {
            onBack.run();
        }
    }
}
