package me.mykindos.betterpvp.core.menu.demo;

import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class DemoMenu extends Menu {
    public DemoMenu(Player player) {
        super(player, 9, ChatColor.RED + "DEMO MENU");
        addButton(new SomeButton());
        addButton(new SomeOtherButton());
    }

    private static class SomeButton extends Button {

        public SomeButton() {
            super(3, new ItemStack(Material.DIAMOND), "Click me", "What is the worst that could happen?");
        }

        @Override
        public void onClick(Player player, ClickType clickType) {
            player.setHealth(0);
            UtilSound.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_5, 0.1f, 1, false);
            UtilSound.playSound(player.getWorld(), player.getLocation(), Sound.MUSIC_DISC_PIGSTEP, 0.1f, 1);
            UtilSound.playSound(player, Sound.MUSIC_DISC_OTHERSIDE, 0.1f, 1, true);
        }
    }

    private static class SomeOtherButton extends Button {

        public SomeOtherButton() {
            super(4, new ItemStack(Material.DIAMOND), "Click me", "I heard you like music");
        }

        @Override
        public void onClick(Player player, ClickType clickType) {

            UtilSound.playSound(player, Sound.MUSIC_DISC_OTHERSIDE, 0.1f, 1, true);
        }
    }
}
