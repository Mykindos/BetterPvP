package me.mykindos.betterpvp.clans.world.navigation;

import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.travel.TravelService;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A clickable entry in the {@link NavigationMenu} for one {@link Destination}. Left-clicking closes the menu and
 * hands the traveller off to {@link TravelService}.
 */
public class DestinationButton extends ControlItem<NavigationMenu> {

    private final Destination destination;
    private final TravelService travelService;

    public DestinationButton(Destination destination, TravelService travelService) {
        this.destination = destination;
        this.travelService = travelService;
    }

    @Override
    public ItemProvider getItemProvider(NavigationMenu menu) {
        return destination.icon();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (!clickType.isLeftClick()) {
            return;
        }

        player.closeInventory();
        travelService.travel(player, destination);
    }
}
