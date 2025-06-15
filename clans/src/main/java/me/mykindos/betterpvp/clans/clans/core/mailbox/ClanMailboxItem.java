package me.mykindos.betterpvp.clans.clans.core.mailbox;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.UUIDProperty;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@CustomLog
public class ClanMailboxItem extends ControlItem<GuiClanMailbox> {

    private final ClanMailbox clanMailbox;
    private final ItemStack itemStack;

    private final ItemFactory itemFactory;

    public ClanMailboxItem(ClanMailbox clanMailbox, ItemStack itemStack, ItemFactory itemFactory) {
        this.clanMailbox = clanMailbox;
        this.itemStack = itemStack;
        this.itemFactory = itemFactory;
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
        if (player.getInventory().firstEmpty() == -1) {
            UtilMessage.simpleMessage(player, "Mailbox", "You cannot withdraw items while your inventory is full.");
            return;
        }
        if (clickType.isLeftClick()) {

            if(itemStack.getType().isAir()) {
                return;
            }

            if (clanMailbox.getContents().remove(itemStack)) {
                Inventory clickedInventory = event.getClickedInventory();
                if(clickedInventory != null) {
                    clickedInventory.setItem(event.getSlot(), null);
                }
                player.getInventory().addItem(itemStack);

                itemFactory.fromItemStack(itemStack).ifPresent(itemInstance -> {
                    itemInstance.getComponent(UUIDProperty.class).ifPresent(uuidProperty -> {
                        final UUID uuid = uuidProperty.getUniqueId();
                        Location location = clanMailbox.getClan().getCore().getPosition();
                        log.info("{} retrieved ({}) from {} at {}", player.getName(), uuid, "Clan Mailbox", UtilWorld.locationToString(location))
                                .setAction("ITEM_RETRIEVE")
                                .addClientContext(player)
                                .addLocationContext(location)
                                .addItemContext(itemFactory.getItemRegistry(), itemInstance)
                                .addBlockContext(Objects.requireNonNull(location).getBlock()).submit();
                    });
                });

                new GuiClanMailbox(clanMailbox, itemFactory, null).show(player);
                //new GuiClanMailbox(clanMailbox, itemHandler, null).show(player);
            } else {
                log.warn("Failed to remove item from mailbox for {} ({}).", player.getName(), player.getUniqueId()).submit();
            }
        }
    }
}
