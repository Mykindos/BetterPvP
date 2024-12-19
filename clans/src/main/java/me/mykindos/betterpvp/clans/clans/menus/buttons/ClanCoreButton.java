package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

public class ClanCoreButton extends AbstractItem {

    private final boolean admin;

    public ClanCoreButton(boolean admin) {
        this.admin = admin;
    }

    @Override
    public ItemProvider getItemProvider() {
        final ItemView.ItemViewBuilder provider = ItemView.builder().material(ClanCore.CORE_BLOCK)
                .displayName(Component.text("Clan Core", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .action(ClickActions.ALL, Component.text("Teleport to Core"));
        if (admin) {
            provider.action(ClickActions.SHIFT, Component.text("Set Core"));
        }
        return provider.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        player.closeInventory(InventoryCloseEvent.Reason.TELEPORT);
        if (admin && ClickActions.SHIFT.accepts(clickType)) {
            player.chat("/clan setcore");
        } else {
            player.chat("/clan core");
        }
    }
}
