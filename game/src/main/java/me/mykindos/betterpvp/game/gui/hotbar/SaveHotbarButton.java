package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class SaveHotbarButton extends ControlItem<HotBarEditor> {
    @Override
    public ItemProvider getItemProvider(HotBarEditor gui) {
        return ItemView.builder()
                .displayName(Component.text("Save", NamedTextColor.GREEN))
                .material(Material.GREEN_STAINED_GLASS)
                .action(ClickActions.ALL, Component.text("Save Hotbar"))
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
// Only save if the layout has changed, so we don't spam the DB
        if (!getGui().getOriginal().equals(getGui().getInProgress())) {
            getGui().getOriginal().copy(getGui().getInProgress());
            getGui().getHotBarLayoutManager().saveLayout(player, getGui().getInProgress());
        }
        player.closeInventory();
        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f).play(player);
        getGui().getOnSave().accept(player);
    }
}
