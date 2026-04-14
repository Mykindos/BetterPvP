package me.mykindos.betterpvp.shops.shops.menus.buttons.direction;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class BackToPreviousButton extends AbstractItem {

    private final Windowed previous;
    private final boolean invisible;

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(invisible
                        ? Resources.ItemModel.INVISIBLE
                        : Key.key("betterpvp", "menu/gui/shop/page_backward"))
                .displayName(Component.text("Back", NamedTextColor.GREEN))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        previous.show(player);
    }
}
