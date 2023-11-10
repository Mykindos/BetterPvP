package me.mykindos.betterpvp.core.menu.button;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

@RequiredArgsConstructor
@AllArgsConstructor
public class BackButton extends AbstractItem {

    private final Windowed previousMenu;
    private ItemProvider fallback = Menu.BACKGROUND_ITEM;

    @Override
    public ItemProvider getItemProvider() {
        if (previousMenu == null) {
            return fallback;
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
        previousMenu.show(player);
    }
}
