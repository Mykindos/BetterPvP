package me.mykindos.betterpvp.core.client.stats.display.general;

import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class DetailedStatsButton extends ControlItem<IAbstractStatMenu> {
    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {

        return ItemView.builder()
                .material(Material.WRITABLE_BOOK)
                .displayName(Component.text("Detailed Stats"))
                .action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .build();
    }


    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final IAbstractStatMenu gui = getGui();
        new DetailedStatListMenu(gui.getClient(), gui, gui.getType(), gui.getPeriod(), gui.getRealmManager()).show(player);

    }
}
