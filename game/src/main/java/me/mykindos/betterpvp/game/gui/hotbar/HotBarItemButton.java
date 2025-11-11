package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class HotBarItemButton extends ControlItem<HotBarEditor> {

    private final ItemFactory itemFactory;
    private final HotBarItem hotBarItem;

    public HotBarItemButton(ItemFactory itemFactory, HotBarItem hotBarItem) {
        this.itemFactory = itemFactory;
        this.hotBarItem = hotBarItem;
    }

    @Override
    public ItemProvider getItemProvider(HotBarEditor gui) {
        final BaseItem baseItem = Objects.requireNonNull(itemFactory.getItemRegistry().getItem(hotBarItem.getNamespacedKey()));
        final ItemInstance instance = itemFactory.create(baseItem);
        final ItemStack itemStack = instance.createItemStack();
        itemStack.setAmount(hotBarItem.getAmount());

        final ItemView.ItemViewBuilder builder = ItemView.of(itemStack).toBuilder();

        // Display name
        builder.displayName(instance.getView().getName()
                .appendSpace()
                .append(Component.text("●", NamedTextColor.GRAY))
                .appendSpace()
                .append(Component.text(String.format("%d Item Tokens", hotBarItem.getTokenCost()), NamedTextColor.GOLD)))
                .amount(hotBarItem.getAmount());

        // Lore and fallback
        if (!getGui().getInProgress().canAddItem(hotBarItem)) {
            builder.lore(Component.empty());
            builder.lore(Component.text("Not enough item tokens!", TextColor.color(255, 0, 0)));
        } else {
            builder
                    .amount(hotBarItem.getAmount())
                    .action(ClickActions.ALL, Component.text("Add"));
        }

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
        if (getGui().getInProgress().canAddItem(hotBarItem)) {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_HARP, 1.5f, 1.0f).play(player);
            getGui().getInProgress().setSlot(getGui().getSelectedSlot(), hotBarItem);
            getGui().incrementSelectedSlot();
            getGui().updateControlItems();
        } else {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, 1.0f).play(player);
        }
    }
}
