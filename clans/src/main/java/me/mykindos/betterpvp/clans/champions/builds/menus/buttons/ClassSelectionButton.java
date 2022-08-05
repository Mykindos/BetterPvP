package me.mykindos.betterpvp.clans.champions.builds.menus.buttons;

import me.mykindos.betterpvp.clans.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.SkillManager;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ClassSelectionButton extends Button {

    private final Gamer gamer;
    private final Role role;
    private final SkillManager skillManager;


    public ClassSelectionButton(Gamer gamer, Role role, SkillManager skillManager, int slot, ItemStack item) {
        super(slot, item, ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName());
        this.gamer = gamer;
        this.role = role;
        this.skillManager = skillManager;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        MenuManager.openMenu(player, new BuildMenu(player, gamer, role, skillManager));
    }
}
