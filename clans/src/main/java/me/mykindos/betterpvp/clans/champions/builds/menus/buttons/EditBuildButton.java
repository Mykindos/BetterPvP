package me.mykindos.betterpvp.clans.champions.builds.menus.buttons;

import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class EditBuildButton extends Button {

    private final RoleBuild roleBuild;

    public EditBuildButton(RoleBuild roleBuild, int slot, ItemStack item, String name, String... lore) {
        super(slot, item, name, lore);
        this.roleBuild = roleBuild;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        MenuManager.openMenu(player, new SkillMenu(player, roleBuild));
    }
}
