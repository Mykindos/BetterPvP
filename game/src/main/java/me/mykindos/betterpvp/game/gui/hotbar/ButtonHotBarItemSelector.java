package me.mykindos.betterpvp.game.gui.hotbar;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarItem;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class ButtonHotBarItemSelector extends AbstractItem {

    private final ItemHandler itemHandler;
    private final HotBarLayout hotBarLayout;
    private final int slot;
    private final HotBarItem hotBarItem;
    private final Windowed previous;

    @Override
    public ItemProvider getItemProvider() {
        final BPvPItem item = itemHandler.getItem(hotBarItem.getNamespacedKey());
        final ItemView.ItemViewBuilder builder = ItemView.of(item.getItemStack()).toBuilder();

        // Display name
        builder.displayName(item.getName()
                .appendSpace()
                .append(Component.text("●", NamedTextColor.GRAY))
                .appendSpace()
                .append(Component.text(String.format("%d Item Tokens", hotBarItem.getTokenCost()), NamedTextColor.GOLD)));

        // Lore and fallback
        if (!hotBarLayout.canAddItem(hotBarItem)) {
            // todo: add an X instead of whatever this is
            builder.material(Material.RED_CONCRETE);
            builder.lore(Component.empty());
            builder.lore(Component.text("Not enough item tokens!", TextColor.color(255, 0, 0)));
        } else {
            builder
                    .amount(hotBarItem.getAmount())
                    .action(ClickActions.ALL, Component.text("Select"));
        }

        return builder
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (hotBarLayout.canAddItem(hotBarItem)) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_HARP, 1.5f, 1.0f).play(player);
            hotBarLayout.setSlot(slot, hotBarItem);
            previous.show(player);
        } else {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, 1.0f).play(player);
        }
    }
}
