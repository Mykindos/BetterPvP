package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ResetHotbarButton extends ControlItem<HotBarEditor> {
    @Override
    public ItemProvider getItemProvider(HotBarEditor gui) {
        return ItemView.builder()
                .displayName(Component.text("Reset", NamedTextColor.GREEN))
                .material(Material.TINTED_GLASS)
                .action(ClickActions.ALL, Component.text("Reset Hotbar"))
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
        new ConfirmationMenu("Reset hotbar layout", success -> {
            if (Boolean.FALSE.equals(success)) return;

            final HotBarLayout defaultLayout = HotBarLayoutManager.getDefaultHotbarLayout(getGui().getInProgress().getBuild(), getGui().getInProgress().getMaxTokens());
            getGui().getInProgress().copy(defaultLayout);
            getGui().setSelectedSlot(-1);
            getGui().incrementSelectedSlot();
            getGui().updateControlItems();
            getGui().show(player);
        }).show(player);
    }
}
