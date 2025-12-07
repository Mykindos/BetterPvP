package me.mykindos.betterpvp.core.client.rewards;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
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
public class RewardBoxItem extends ControlItem<GuiRewardBox> {

    private final RewardBox rewardBox;
    private final ItemStack itemStack;

    private final ItemFactory itemFactory;

    public RewardBoxItem(RewardBox rewardBox, ItemStack itemStack, ItemFactory itemFactory) {
        this.rewardBox = rewardBox;
        this.itemStack = itemStack;
        this.itemFactory = itemFactory;
    }

    @Override
    public ItemProvider getItemProvider(GuiRewardBox gui) {
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
            UtilMessage.simpleMessage(player, "Rewards", "You cannot withdraw items while your inventory is full.");
            return;
        }
        if (clickType.isLeftClick()) {

            if (rewardBox.getContents().remove(itemStack)) {
                Inventory clickedInventory = event.getClickedInventory();
                if(clickedInventory != null) {
                    clickedInventory.setItem(event.getSlot(), null);
                }
                player.getInventory().addItem(itemStack);

                itemFactory.fromItemStack(itemStack).ifPresent(item -> {
                    item.getComponent(UUIDProperty.class).ifPresent(uuidProperty -> {
                        final UUID uuid = uuidProperty.getUniqueId();
                        final Location location = player.getLocation();
                        log.info("{} retrieved ({}) from {} at {}",
                                        player.getName(),
                                        uuid,
                                        "Reward Box",
                                        UtilWorld.locationToString(location))
                                .setAction("ITEM_RETRIEVE").addClientContext(player).addLocationContext(location).addItemContext(itemFactory.getItemRegistry(), item)
                                .addBlockContext(Objects.requireNonNull(location).getBlock()).submit();
                    });
                });
            } else {
                player.closeInventory();
                log.error("Failed to remove item from mailbox for {} ({}).", player.getName(), player.getUniqueId()).submit();
            }
        }
    }
}