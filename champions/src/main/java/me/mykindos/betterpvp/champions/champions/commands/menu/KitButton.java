package me.mykindos.betterpvp.champions.champions.commands.menu;

import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class KitButton extends SimpleItem {

    private final Role role;
    private final ItemHandler itemHandler;

    public KitButton(ItemView item, Role role, ItemHandler itemHandler) {
        super(item);
        this.role = role;
        this.itemHandler = itemHandler;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        role.equip(itemHandler, player);

        if (!player.getInventory().contains(Material.BOOK)) {
            player.getInventory().addItem(itemHandler.updateNames(new ItemStack(Material.BOOK)));
        }

        ItemStack stews = new ItemStack(Material.PUMPKIN_PIE, 16);
        stews.editMeta(meta -> meta.setCustomModelData(1));
        UtilItem.insert(player, itemHandler.updateNames(stews));

        player.closeInventory();
    }

}
