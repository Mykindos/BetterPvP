package me.mykindos.betterpvp.clans.champions.commands.menu;

import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.core.menu.Menu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KitMenu extends Menu {

    public KitMenu(Player player) {
        super(player, 36, ChatColor.RED + "Select a kit");

        int[] start = new int[]{0, 1, 3, 5, 7, 8};
        int count = 0;
        for (Role role : Role.values()) {
            addButton(new KitButton(start[count], new ItemStack(role.getHelmet()).clone(), ChatColor.GREEN + role.getName(), role));
            addButton(new KitButton(start[count] + 9, new ItemStack(role.getChestplate()).clone(), ChatColor.GREEN + role.getName(), role));
            addButton(new KitButton(start[count] + 18, new ItemStack(role.getLeggings()).clone(), ChatColor.GREEN + role.getName(), role));
            addButton(new KitButton(start[count] + 27, new ItemStack(role.getBoots()).clone(), ChatColor.GREEN + role.getName(), role));
            count++;
        }
    }
}
