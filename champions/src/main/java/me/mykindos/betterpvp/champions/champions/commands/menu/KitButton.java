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
        player.getInventory().setHelmet(itemHandler.updateNames(new ItemStack(role.getHelmet())));
        player.getInventory().setChestplate(itemHandler.updateNames(new ItemStack(role.getChestplate())));
        player.getInventory().setLeggings(itemHandler.updateNames(new ItemStack(role.getLeggings())));
        player.getInventory().setBoots(itemHandler.updateNames(new ItemStack(role.getBoots())));

        if (!player.getInventory().contains(Material.IRON_SWORD)) {
            player.getInventory().addItem(itemHandler.updateNames(new ItemStack(Material.IRON_SWORD)));
        }

        if (!player.getInventory().contains(Material.IRON_AXE)) {
            player.getInventory().addItem(itemHandler.updateNames(new ItemStack(Material.IRON_AXE)));
        }

        if((role == Role.ASSASSIN) || (role == Role.RANGER)) {
            if (!player.getInventory().contains(Material.BOW)) {
                player.getInventory().addItem(itemHandler.updateNames(new ItemStack(Material.BOW)));
            }
            int numArrows = (role == Role.RANGER ? 64 : 32);
            player.getInventory().addItem(itemHandler.updateNames(new ItemStack(Material.ARROW, numArrows)));
        }
        if (!player.getInventory().contains(Material.BOOK)) {
            player.getInventory().addItem(itemHandler.updateNames(new ItemStack(Material.BOOK)));
        }

        ItemStack stews = new ItemStack(Material.PUMPKIN_PIE, 16);
        stews.editMeta(meta -> meta.setCustomModelData(1));
        UtilItem.insert(player, itemHandler.updateNames(stews));

        player.closeInventory();
    }

}
