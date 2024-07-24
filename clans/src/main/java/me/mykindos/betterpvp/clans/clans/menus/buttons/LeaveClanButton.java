package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class LeaveClanButton extends AbstractItem {

    private final boolean leader;

    public LeaveClanButton(boolean leader) {
        this.leader = leader;
    }

    @Override
    public ItemProvider getItemProvider() {
        final ItemView.ItemViewBuilder provider = ItemView.builder()
                .material(Material.PAPER)
                .customModelData(5)
                .displayName(Component.text("Leave", NamedTextColor.RED))
                .action(ClickActions.ALL, Component.text("Leave Clan"));
        if (leader) {
            provider.action(ClickActions.SHIFT, Component.text("Disband Clan"));
        }
        return provider.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (leader && ClickActions.SHIFT.accepts(clickType)) {
            player.closeInventory();
            player.chat("/clan disband");
        } else if (ClickActions.LEFT.accepts(clickType)) {
            player.closeInventory();
            player.chat("/clan leave");
        }
    }

}

