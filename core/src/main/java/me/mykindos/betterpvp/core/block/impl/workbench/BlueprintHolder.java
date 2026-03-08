package me.mykindos.betterpvp.core.block.impl.workbench;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintComponent;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@RequiredArgsConstructor
public class BlueprintHolder extends AbstractItem {

    private final ItemFactory itemFactory;
    private final VirtualInventory virtualInventory;

    @Override
    public ItemProvider getItemProvider() {
        final ItemStack item = virtualInventory.getItem(0);
        if (item == null) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .displayName(Component.text("Add Blueprint", NamedTextColor.GREEN))
                    .lore(Component.text("Drag a blueprint from your", NamedTextColor.GRAY))
                    .lore(Component.text("inventory into this slot to", NamedTextColor.GRAY))
                    .lore(Component.text("use it.", NamedTextColor.GRAY))
                    .build();
        }

        return ItemView.of(itemFactory.fromItemStack(item)
                .orElseThrow()
                .getView().get())
                .toBuilder()
                .action(ClickActions.LEFT, Component.text("Remove"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        ItemStack cursorItem = event.getCursor();
        if (clickType != ClickType.LEFT) {
            player.updateInventory();
            return;
        }

        // If there's already a blueprint in
        if (!virtualInventory.isEmpty()) {
            // Refund them if they have an empty cursor
            if (cursorItem.isEmpty()) {
                event.setCursor(virtualInventory.getItem(0));
                virtualInventory.setItem(new PlayerUpdateReason(player, event), 0, null);
                notifyWindows();
            }
            return;
        }

        // Try to convert the cursor item to ItemInstance and check if it's a blueprint
        final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(cursorItem);
        if (instanceOpt.isEmpty()) {
            return;
        }

        final ItemInstance itemInstance = instanceOpt.get();
        final Optional<BlueprintComponent> componentOpt = itemInstance.getComponent(BlueprintComponent.class);
        if (componentOpt.isEmpty()) {
            // Not a blueprint
            return;
        }

        // At this point it's a valid blueprint
        // Create a copy of the item and place it
        ItemStack singleItem = cursorItem.clone();
        singleItem.setAmount(1);
        virtualInventory.setItem(new PlayerUpdateReason(player, event), 0, singleItem);

        // Reduce cursor item by 1
        if (cursorItem.getAmount() > 1) {
            cursorItem.setAmount(cursorItem.getAmount() - 1);
            event.setCursor(cursorItem);
        } else {
            event.setCursor(null);
        }

        // Update all GUI sections to reflect the addition
        notifyWindows();
    }
}
