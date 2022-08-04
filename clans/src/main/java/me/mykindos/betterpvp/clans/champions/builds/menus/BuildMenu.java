package me.mykindos.betterpvp.clans.champions.builds.menus;

import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.builds.menus.buttons.ApplyBuildButton;
import me.mykindos.betterpvp.clans.champions.builds.menus.buttons.DeleteBuildButton;
import me.mykindos.betterpvp.clans.champions.builds.menus.buttons.EditBuildButton;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.gamer.Gamer;
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

    private final Gamer gamer;
    private final Role role;

    public BuildMenu(Player player, Gamer gamer, Role role) {
        super(player, 54, ChatColor.GREEN.toString() + ChatColor.BOLD + role.getName() + " builds");
        this.gamer = gamer;
        this.role = role;

        refresh();
    }

    @Override
    public void refresh() {
        addButton(new Button(0, new ItemStack(Material.EMERALD_BLOCK), ChatColor.GREEN.toString() + ChatColor.BOLD + "Back"));
        addButton(new Button(18, new ItemStack(role.getHelmet()), ChatColor.GREEN.toString() + ChatColor.BOLD + role + " Helmet"));
        addButton(new Button(27, new ItemStack(role.getChestplate()), ChatColor.GREEN.toString() + ChatColor.BOLD + role + " Chestplate"));
        addButton(new Button(36, new ItemStack(role.getLeggings()), ChatColor.GREEN.toString() + ChatColor.BOLD + role + " Leggings"));
        addButton(new Button(45, new ItemStack(role.getBoots()), ChatColor.GREEN.toString() + ChatColor.BOLD + role + " Boots"));

        int slot = 9;


        for (int i = 0; i < 4; i++) {

            RoleBuild activeBuild = gamer.getActiveBuilds().get(role.getName());
            if (activeBuild == null) {
                activeBuild = new RoleBuild(gamer.getUuid(), role, i+1);
            }
            if (activeBuild.getId() == i + 1) {
                addButton(new ApplyBuildButton(gamer, slot + 11, UtilItem.addGlow(getApplyBuildItem(i + 1)), ChatColor.GREEN.toString() + ChatColor.BOLD + "Apply Build - " + (i + 1)));
            } else {
                addButton(new ApplyBuildButton(gamer, slot + 11, getApplyBuildItem(i + 1), ChatColor.GREEN.toString() + ChatColor.BOLD + "Apply Build - " + (i + 1)));
            }
            addButton(new EditBuildButton(activeBuild, slot + 20, new ItemStack(Material.ANVIL), ChatColor.GREEN.toString() + ChatColor.BOLD + "Edit & Save Build - " + (i + 1)));
            addButton(new DeleteBuildButton(gamer, activeBuild, slot + 38, new ItemStack(Material.TNT), ChatColor.GREEN.toString() + ChatColor.BOLD + "Delete Build - " + (i + 1)));

            slot += 2;
        }
    }

    @NotNull
    private ItemStack getApplyBuildItem(int id) {

        return switch (id) {
            case 1 -> new ItemStack(Material.INK_SAC, 1);
            case 2 -> new ItemStack(Material.RED_DYE, 1);
            case 3 -> new ItemStack(Material.GREEN_DYE, 1);
            case 4 -> new ItemStack(Material.CYAN_DYE, 1);
            default -> throw new IllegalStateException("Unexpected value: " + id);
        };

    }
}
