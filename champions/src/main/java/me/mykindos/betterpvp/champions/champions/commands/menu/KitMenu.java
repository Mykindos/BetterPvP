package me.mykindos.betterpvp.champions.champions.commands.menu;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KitMenu extends Menu {

    public KitMenu(Player player, RoleManager roleManager) {
        super(player, 36, Component.text("Select a kit", NamedTextColor.RED));

        int[] start = new int[]{0, 1, 3, 5, 7, 8};
        int count = 0;
        for (Role role : roleManager.getRoles()) {
            addButton(new KitButton(start[count], new ItemStack(role.getHelmet()).clone(), role.getName(), role));
            addButton(new KitButton(start[count] + 9, new ItemStack(role.getChestplate()).clone(), role.getName(), role));
            addButton(new KitButton(start[count] + 18, new ItemStack(role.getLeggings()).clone(), role.getName(), role));
            addButton(new KitButton(start[count] + 27, new ItemStack(role.getBoots()).clone(), role.getName(), role));
            count++;
        }
    }
}
