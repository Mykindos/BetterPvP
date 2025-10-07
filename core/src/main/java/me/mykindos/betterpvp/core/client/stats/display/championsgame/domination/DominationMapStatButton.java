package me.mykindos.betterpvp.core.client.stats.display.championsgame.domination;

import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.championsgame.domination.map.DominationMapStatMenu;
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

public class DominationMapStatButton extends ControlItem<AbstractStatMenu> {


    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @param gui
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider(AbstractStatMenu gui) {

        return ItemView.builder()
                .material(Material.MAP)
                .displayName(Component.text("Map Specific Stats"))
                .frameLore(true)
                .action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .build();
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final AbstractStatMenu gui = getGui();
        new DominationMapStatMenu(gui.getClient(), gui, gui.getPeriodKey(), gui.getStatPeriodManager()).show(player);
    }
}
