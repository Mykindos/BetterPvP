package me.mykindos.betterpvp.clans.champions.builds.menus.buttons;

import me.mykindos.betterpvp.clans.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ClassSelectionButton extends Button {

    private final Gamer gamer;

    public ClassSelectionButton(Gamer gamer, int slot, ItemStack item, String name, String... lore) {
        super(slot, item, name, lore);
        this.gamer = gamer;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        String role = ChatColor.stripColor(getName()).split(" ")[0];
        MenuManager.openMenu(player, new BuildMenu(player, gamer, Role.valueOf(role.toUpperCase())));
        //player.openInventory(new BuildPage(getInstance(), p, role).getInventory());
    }
}
