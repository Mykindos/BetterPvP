package me.mykindos.betterpvp.champions.champions.builds.menus;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ApplyBuildButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.DeleteBuildButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.EditBuildButton;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BuildMenu extends Menu implements IRefreshingMenu {

    private final GamerBuilds builds;
    private final Role role;
    private final SkillManager skillManager;

    public BuildMenu(Player player, GamerBuilds builds, Role role, SkillManager skillManager) {
        super(player, 54, ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName() + " builds");
        this.builds = builds;
        this.role = role;
        this.skillManager = skillManager;
        refresh();
    }

    @Override
    public void refresh() {
        addButton(new Button(0, new ItemStack(Material.EMERALD_BLOCK), ChatColor.GREEN.toString() + ChatColor.BOLD + "Back"));
        addButton(new Button(18, new ItemStack(role.getHelmet()), ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName() + " Helmet"));
        addButton(new Button(27, new ItemStack(role.getChestplate()), ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName() + " Chestplate"));
        addButton(new Button(36, new ItemStack(role.getLeggings()), ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName() + " Leggings"));
        addButton(new Button(45, new ItemStack(role.getBoots()), ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName() + " Boots"));

        int slot = 9;


        for (int i = 1; i < 5; i++) {

            RoleBuild activeBuild = builds.getActiveBuilds().get(role.getName());
            addButton(new ApplyBuildButton(builds, role, i, slot + 11, getApplyBuildItem(i, activeBuild.getId() == i)));
            addButton(new EditBuildButton(builds, role, i, skillManager, slot + 20));
            addButton(new DeleteBuildButton(builds, role, i, slot + 38));

            slot += 2;
        }
    }

    @NotNull
    private ItemStack getApplyBuildItem(int id, boolean addGlow) {

        ItemStack itemStack;
        switch (id) {
            case 1 -> itemStack = new ItemStack(Material.INK_SAC, 1);
            case 2 -> itemStack = new ItemStack(Material.RED_DYE, 1);
            case 3 -> itemStack = new ItemStack(Material.GREEN_DYE, 1);
            case 4 -> itemStack = new ItemStack(Material.CYAN_DYE, 1);
            default -> throw new IllegalStateException("Unexpected value: " + id);
        }

        return addGlow ? UtilItem.addGlow(itemStack) : itemStack;
    }
}
