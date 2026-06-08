package me.mykindos.betterpvp.core.client.stats.display.general;

import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
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
                .displayName(Translations.component("core.menu.stats.button.detailed.name"))
                .action(ClickActions.ALL, Translations.component("core.menu.stats.button.show-detailed.action"))
                .build();
    }


    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final IAbstractStatMenu gui = getGui();
        new DetailedStatListMenu(gui.getClient(), gui, gui.getType(), gui.getPeriod(), gui.getRealmManager()).show(player);

    }
}
