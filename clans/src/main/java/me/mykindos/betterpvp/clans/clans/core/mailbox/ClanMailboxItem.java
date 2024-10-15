package me.mykindos.betterpvp.clans.clans.core.mailbox;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CustomLog
public class ClanMailboxItem extends ControlItem<GuiClanMailbox> {

    private final ClanMailbox clanMailbox;
    private final ItemStack itemStack;

    public ClanMailboxItem(ClanMailbox clanMailbox, ItemStack itemStack) {
        this.clanMailbox = clanMailbox;
        this.itemStack = itemStack;
    }

    @Override
    public ItemProvider getItemProvider(GuiClanMailbox gui) {
        ItemStack item = itemStack.clone();
        item.editMeta(meta -> {
            List<Component> lore = meta.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            }

            lore.add(Component.text(""));
            lore.add(UtilMessage.DIVIDER);
            lore.add(Component.text(""));
            lore.add(Component.text(ClickActions.LEFT.getName() + " to ", NamedTextColor.WHITE).append(Component.text("Withdraw", NamedTextColor.YELLOW)));
            meta.lore(lore);
        });

        return ItemView.of(item);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()) {

            if (clanMailbox.getContents().remove(itemStack)) {
                player.getInventory().addItem(itemStack);
                new GuiClanMailbox(clanMailbox, null).show(player);
            } else {
                player.closeInventory();
                log.error("Failed to remove item from mailbox for {} ({}).", player.getName(), player.getUniqueId()).submit();
            }
        }
    }
}
