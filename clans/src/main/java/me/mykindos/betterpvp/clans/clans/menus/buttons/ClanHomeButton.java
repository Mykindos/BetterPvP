package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

public class ClanHomeButton extends AbstractItem {

    private final boolean admin;

    public ClanHomeButton(boolean admin) {
        this.admin = admin;
    }

    @Override
    public ItemProvider getItemProvider() {
        final ItemView.ItemViewBuilder provider = ItemView.builder().material(Material.RED_BED)
                .displayName(Component.text("Clan Home", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .action(ClickActions.ALL, Component.text("Teleport Home"));
        if (admin) {
            provider.action(ClickActions.SHIFT, Component.text("Set Home"));
        }
        return provider.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        player.closeInventory(InventoryCloseEvent.Reason.TELEPORT);
        if (admin && ClickActions.SHIFT.accepts(clickType)) {
            player.chat("/clan sethome");
        } else {
            player.chat("/clan home");
        }
    }
}
