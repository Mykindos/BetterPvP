package me.mykindos.betterpvp.clans.champions.builds.menus.buttons;

import me.mykindos.betterpvp.clans.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.SkillManager;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class EditBuildButton extends Button {

    private final Gamer gamer;
    private final Role role;
    private final int buildNumber;
    private final SkillManager skillManager;

    public EditBuildButton(Gamer gamer, Role role, int buildNumber, SkillManager skillManager, int slot) {
        super(slot, new ItemStack(Material.ANVIL), ChatColor.GREEN.toString() + ChatColor.BOLD + "Edit & Save Build - " + buildNumber);
        this.gamer = gamer;
        this.role = role;
        this.buildNumber = buildNumber;
        this.skillManager = skillManager;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        MenuManager.openMenu(player, new SkillMenu(player, gamer, role, buildNumber, skillManager));
    }
}
