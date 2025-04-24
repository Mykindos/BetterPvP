package me.mykindos.betterpvp.game.gui.hotbar;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.impl.GuiSelectOne;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarItem;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Renders current {@link HotBarLayout} slot, with the ability to change the item in the slot.
 */
@RequiredArgsConstructor
public class ButtonHotBarSlot extends ControlItem<GuiHotBarEditor> {

    private final ItemHandler itemHandler;
    private final HotBarLayout hotBarLayout;
    private final int slot;

    @Override
    public ItemProvider getItemProvider(GuiHotBarEditor gui) {
        final Optional<HotBarItem> itemOpt = hotBarLayout.getSlot(slot);
        if (itemOpt.isEmpty()) {
            return ItemView.builder()
                    .flag(ItemFlag.HIDE_ATTRIBUTES)
                    .material(Material.PAPER)
                    .displayName(Component.text("Empty Slot", NamedTextColor.GRAY))
                    .itemModel(Resources.ItemModel.HOT_BAR_EDITOR_PLUS)
                    .action(ClickActions.ALL, Component.text("Add"))
                    .build();
        } else {
            final HotBarItem hotbarItem = itemOpt.get();
            final BPvPItem item = itemHandler.getItem(hotbarItem.getNamespacedKey());
            final ItemStack count = item.getItemStack(hotbarItem.getAmount());

            return ItemView.of(count).toBuilder()
                    .flag(ItemFlag.HIDE_ATTRIBUTES)
                    .action(ClickActions.LEFT, Component.text("Change"))
                    .action(ClickActions.RIGHT, Component.text("Remove"))
                    .build();
        }
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.4f, 1.0f).play(player);

            final List<Item> buttons = Arrays.stream(HotBarItem.values())
                    .map(item -> (Item) new ButtonHotBarItemSelector(itemHandler, hotBarLayout, slot, item, getGui()))
                    .toList();

            new GuiSelectOne(buttons).show(player);
        } else if (clickType.isRightClick() && hotBarLayout.getSlot(slot).isPresent()) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_HARP, 1.4f, 1.0f).play(player);
            hotBarLayout.removeSlot(slot);
            notifyWindows();
        }
    }
}
