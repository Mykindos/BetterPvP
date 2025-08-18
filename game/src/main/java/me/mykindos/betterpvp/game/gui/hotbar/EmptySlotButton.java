package me.mykindos.betterpvp.game.gui.hotbar;

import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
public class EmptySlotButton extends ControlItem<HotBarEditor> {
    private int normalIndex;
    private int guiIndex;

    @Override
    public ItemProvider getItemProvider(HotBarEditor gui) {
        return ItemView.builder()
                .displayName(Component.text("Empty Slot"))
                .material(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .action(ClickActions.ALL, Component.text("Set Selected"))
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
        getGui().setSelectedSlot(normalIndex);
    }
}
