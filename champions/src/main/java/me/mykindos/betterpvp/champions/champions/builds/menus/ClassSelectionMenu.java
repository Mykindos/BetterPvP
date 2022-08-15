package me.mykindos.betterpvp.champions.champions.builds.menus;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassSelectionButton;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ClassSelectionMenu extends Menu {
    public ClassSelectionMenu(Player player, GamerBuilds builds, SkillManager skillManager) {
        super(player, 36, "Class Customisation");
        load(builds, skillManager);
    }

    private void load(GamerBuilds builds, SkillManager skillManager) {
        int[] start = new int[]{0, 1, 3, 5, 7, 8};
        int count = 0;
        for (Role role : Role.values()) {
            addButton(new ClassSelectionButton(builds, role, skillManager, start[count], new ItemStack(role.getHelmet()).clone()));
            addButton(new ClassSelectionButton(builds, role, skillManager, start[count] + 9, new ItemStack(role.getChestplate()).clone()));
            addButton(new ClassSelectionButton(builds, role, skillManager, start[count] + 18, new ItemStack(role.getLeggings()).clone()));
            addButton(new ClassSelectionButton(builds, role, skillManager, start[count] + 27, new ItemStack(role.getBoots()).clone()));
            count++;
        }
    }
}
