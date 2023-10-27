package me.mykindos.betterpvp.champions.champions.builds.menus;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassSelectionButton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Menu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Iterator;

public class ClassSelectionMenu extends Menu {
    public ClassSelectionMenu(Player player, GamerBuilds builds, SkillManager skillManager, RoleManager roleManager) {
        super(player, 27, Component.text("Pick a Kit"));
        load(builds, skillManager, roleManager);
    }

    private void load(GamerBuilds builds, SkillManager skillManager, RoleManager roleManager) {
        int[] slots = new int[] {10, 11, 12, 14, 15, 16};
        final Iterator<Role> iterator = roleManager.getRoles().iterator();
        for (int slot : slots) {
            if (!iterator.hasNext()) break;
            final Role role = iterator.next();
            addButton(new ClassSelectionButton(builds, role, skillManager, roleManager, slot, new ItemStack(role.getChestplate())));
        }

        fillEmpty(Menu.BACKGROUND);
    }
}
