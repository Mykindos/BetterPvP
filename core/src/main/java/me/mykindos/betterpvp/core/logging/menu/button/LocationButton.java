
package me.mykindos.betterpvp.core.logging.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
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

public class LocationButton extends LogRepositoryButton {
    private final Location location;
    private final boolean admin;


    public LocationButton(Location location, boolean admin, Windowed previous) {
        super();
        this.location = location;
        this.admin = admin;
    }

    @Override
    public ItemProvider getItemProvider() {

        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder()
            .displayName(Component.text("Location: " + location.getWorld().getName()))
            .action(ClickActions.LEFT, Component.text("Send in Chat"))
            .material(Material.IRON_SWORD)
            .customModelData(0)
            .lore(Component.text(UtilWorld.locationToString(location, true, false)))
            .frameLore(true);

        if (admin) {
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
            UtilMessage.message(player, "Location", UtilWorld.locationToString(location));
        }
        if (clickType.isRightClick() && admin) {
            player.teleport(location);
        }
    }
}