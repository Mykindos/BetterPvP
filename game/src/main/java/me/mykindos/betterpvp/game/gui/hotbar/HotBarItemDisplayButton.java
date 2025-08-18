package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class HotBarItemDisplayButton extends ControlItem<HotBarEditor> {

    private final HotBarItem hotBarItem;
    private final int normalIndex;

    public HotBarItemDisplayButton(HotBarItem hotBarItem, int normalIndex) {
        this.hotBarItem = hotBarItem;
        this.normalIndex = normalIndex;
    }

    @Override
    public ItemProvider getItemProvider(HotBarEditor gui) {
        final BPvPItem item = getGui().getItemHandler().getItem(hotBarItem.getNamespacedKey());
        final ItemView.ItemViewBuilder builder = ItemView.of(item.getItemStack()).toBuilder();

        // Display name
        builder.displayName(item.getName()
                .appendSpace()
                .append(Component.text("●", NamedTextColor.GRAY))
                .appendSpace()
                .append(Component.text(String.format("%d Item Tokens", hotBarItem.getTokenCost()), NamedTextColor.GOLD)))
                .amount(hotBarItem.getAmount())
                .action(ClickActions.RIGHT, Component.text("Remove"))
                .action(ClickActions.LEFT, Component.text("Remove + Select"));

        return builder
                .flag(ItemFlag.HIDE_ATTRIBUTES)
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
        if (clickType.isLeftClick()) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.4f, 1.0f).play(player);
            getGui().getInProgress().removeSlot(normalIndex);
            getGui().setSelectedSlot(normalIndex);
            getGui().updateControlItems();

        }
        if (clickType.isRightClick()) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_HARP, 1.4f, 1.0f).play(player);
            getGui().getInProgress().removeSlot(normalIndex);
            getGui().updateControlItems();
        }

    }
}
