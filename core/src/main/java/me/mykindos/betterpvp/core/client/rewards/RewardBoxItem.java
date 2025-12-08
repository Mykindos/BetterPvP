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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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
        return ItemView.of(itemFactory.fromItemStack(itemStack).orElseThrow().getView().get()).toBuilder()
                .action(ClickActions.ALL, Component.text("Claim"))
                .build();
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