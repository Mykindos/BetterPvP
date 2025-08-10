package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TokensButton extends ControlItem<HotBarEditor> {
    @Override
    public ItemProvider getItemProvider(HotBarEditor gui) {
        final HotBarLayout inProgress = gui.getInProgress();
        int max = inProgress.getMaxTokens();
        int remaining = inProgress.getRemainingTokens();
        return ItemView.builder()
                .displayName(UtilMessage.deserialize("<green>%s</green>/<yellow>%s</yellow>", remaining, max))
                .material(Material.GOLD_INGOT)
                .lore(
                        List.of(Component.text("Number of item tokens remaining"))
                )
                .amount(remaining == 0 ? 1 : remaining)
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
        //does nothing
    }
}
