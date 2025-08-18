package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SelectedSlotButton extends FlashingButton<HotBarEditor> {

    @Override
    public ItemProvider getItemProvider(HotBarEditor gui) {
        List<Component> lore = List.of(
                Component.text("Click the inventory above", NamedTextColor.WHITE),
                Component.text("To select an item for this slot", NamedTextColor.WHITE)
        );
        ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.LIME_STAINED_GLASS)
                .displayName(Component.text("Selected Slot", NamedTextColor.GREEN))
                .lore(lore)
                .glow(this.isFlash());
        return builder.build();
    }

    @Override
    public boolean isFlashing() {
        return getGui().getInProgress().getRemainingTokens() > 0;
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
        //this button does nothing when clicked
    }
}