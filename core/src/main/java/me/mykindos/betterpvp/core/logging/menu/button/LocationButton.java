
package me.mykindos.betterpvp.core.logging.menu.button;

import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.PreviousableButton;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class LocationButton extends AbstractItem implements PreviousableButton {
    private final Location location;
    private final boolean admin;
    @Setter
    private Windowed previous;


    public LocationButton(Location location, boolean admin, Windowed previous) {
        super();
        this.location = location;
        this.admin = admin;
        this.previous = previous;
    }

    @Override
    public ItemProvider getItemProvider() {


        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder()
            .action(ClickActions.LEFT, Component.text("Send in Chat"))
            .material(Material.GRASS_BLOCK)
            .customModelData(0)
            .lore(Component.text(UtilWorld.locationToString(location, true, false)))
            .frameLore(true);

        if (location.getWorld() != null) {
            itemViewBuilder.displayName(Component.text("Location: " + location.getWorld().getName()));
        } else {
            itemViewBuilder.displayName(Component.text("Location: " + "Unloaded World"));
        }

        if (admin && location.getWorld() != null) {
            itemViewBuilder.action(ClickActions.RIGHT, Component.text("Teleport"));
        }

        return itemViewBuilder.build();
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
        if (clickType.isLeftClick()) {
            // for mods that give waypoints
            UtilMessage.message(player, "Location", UtilWorld.locationToString(location));
        }
        if (clickType.isRightClick() && admin && location.getWorld() != null) {
            player.teleport(location.toCenterLocation());
        }
    }
}