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

    private final Player player;
    private final int size;
    private final String title;
    private final List<Button> buttons;
    private final Inventory inventory;
    private final long openTime;

    public Menu(Player player, int size, String title, Button[] buttons) {
        this.player = player;
        this.size = size;
        this.title = title;
        this.buttons = Arrays.asList(buttons);
        this.inventory = Bukkit.createInventory(player, size, Component.text(title));
        this.openTime = System.currentTimeMillis();

        fillInventoryWithAir();

        for (Button button : buttons) {
            inventory.setItem(button.getSlot(), button.getItemStack());
        }

    }

    public Menu(Player player, int size, String title) {
        this.player = player;
        this.size = size;
        this.title = title;
        this.buttons = new ArrayList<>();
        this.inventory = Bukkit.createInventory(player, size, Component.text(title));
        this.openTime = System.currentTimeMillis();

        fillInventoryWithAir();
    }

    public void addButton(Button button) {
        buttons.add(button);
        inventory.setItem(button.getSlot(), button.getItemStack());
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

}
