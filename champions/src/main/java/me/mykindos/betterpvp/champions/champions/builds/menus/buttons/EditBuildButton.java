package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class EditBuildButton extends Button {

    private final GamerBuilds builds;
    private final Role role;
    private final int buildNumber;
    private final SkillManager skillManager;

    public EditBuildButton(GamerBuilds builds, Role role, int buildNumber, SkillManager skillManager, int slot) {
        super(slot, new ItemStack(Material.ANVIL), ChatColor.GREEN.toString() + ChatColor.BOLD + "Edit & Save Build - " + buildNumber);
        this.builds = builds;
        this.role = role;
        this.buildNumber = buildNumber;
        this.skillManager = skillManager;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        MenuManager.openMenu(player, new SkillMenu(player, builds, role, buildNumber, skillManager));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
    }
}
