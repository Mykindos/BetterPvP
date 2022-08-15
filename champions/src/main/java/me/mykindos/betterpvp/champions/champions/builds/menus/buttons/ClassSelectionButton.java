package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ClassSelectionButton extends Button {

    private final GamerBuilds builds;
    private final Role role;
    private final SkillManager skillManager;


    public ClassSelectionButton(GamerBuilds builds, Role role, SkillManager skillManager, int slot, ItemStack item) {
        super(slot, item, ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName());
        this.builds = builds;
        this.role = role;
        this.skillManager = skillManager;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        MenuManager.openMenu(player, new BuildMenu(player, builds, role, skillManager));
    }
}
