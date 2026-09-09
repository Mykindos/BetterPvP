package me.mykindos.betterpvp.core.world.travel;

import me.mykindos.betterpvp.core.world.travel.Destination;
import me.mykindos.betterpvp.core.world.travel.TravelService;
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

    /** Whether to skip the departure hold — see {@link NavigationMenu#NavigationMenu(java.util.List, TravelService, boolean)}. */
    private final boolean immediate;

    public DestinationButton(Destination destination, TravelService travelService) {
        this(destination, travelService, false);
    }

    public DestinationButton(Destination destination, TravelService travelService, boolean immediate) {
        this.destination = destination;
        this.travelService = travelService;
        this.immediate = immediate;
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
        if (immediate) {
            travelService.travelNow(player, destination);
        } else {
            travelService.travel(player, destination);
        }
    }
}
