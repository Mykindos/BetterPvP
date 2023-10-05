package me.mykindos.betterpvp.core.menu;

import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public abstract class Menu {

    protected final Player player;
    private final int size;
    private final Component title;
    private final List<Button> buttons;
    private final Inventory inventory;
    private final long openTime;

    public Menu(Player player, int size, Component title, Button[] buttons) {
        this.player = player;
        this.size = size;
        this.title = title;
        this.buttons = Arrays.asList(buttons);
        this.inventory = Bukkit.createInventory(player, size, title);
        this.openTime = System.currentTimeMillis();

        fillInventoryWithAir();
        construct();

    }

    public Menu(Player player, int size, Component title) {
        this.player = player;
        this.size = size;
        this.title = title;
        this.buttons = new ArrayList<>();
        this.inventory = Bukkit.createInventory(player, size, title);
        this.openTime = System.currentTimeMillis();

        fillInventoryWithAir();
    }

    public void construct(){
        for (Button button : buttons) {
            inventory.setItem(button.getSlot(), button.getItemStack());
        }
    }

    public void addButton(Button button) {
        buttons.add(button);
        inventory.setItem(button.getSlot(), button.getItemStack());
    }

    public void refreshButton(Button button) {
        if (buttons.contains(button)) {
            inventory.setItem(button.getSlot(), button.getItemStack());
        }
    }

    public boolean isButton(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                for (Button button : this.getButtons()) {
                    if (button.getItemStack().equals(item)) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    public Button getButton(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                for (Button button : this.getButtons()) {
                    if (button.getItemStack().equals(item)) {
                        return button;
                    }
                }

            }
        }
        return null;
    }

    /**
     * I dont remember why I do this but I don't want to find out
     */
    private void fillInventoryWithAir(){
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, new ItemStack(Material.AIR));
        }
    }

    protected void fillEmpty(ItemStack itemStack) {
        for (int i = 0; i < size; i++) {
            final ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                inventory.setItem(i, itemStack);
            }
        }
    }

}
