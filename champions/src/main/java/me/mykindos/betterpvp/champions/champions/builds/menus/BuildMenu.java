package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ApplyBuildButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.DeleteBuildButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.EditBuildButton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.buttons.BackButton;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class BuildMenu extends Menu implements IRefreshingMenu {

    private final GamerBuilds builds;
    private final Role role;
    private final SkillManager skillManager;

    private final RoleManager roleManager;


    public BuildMenu(Player player, GamerBuilds builds, Role role, SkillManager skillManager, RoleManager roleManager) {
        super(player, 45, Component.text(role.getName() + " Builds"));
        this.builds = builds;
        this.role = role;
        this.skillManager = skillManager;
        this.roleManager = roleManager;
        refresh();
    }

    @Override
    public void refresh() {
        addButton(new BackButton(0, new ItemStack(Material.ARROW), new ClassSelectionMenu(player, builds, skillManager, roleManager)));
        addButton(new Button(9, new ItemStack(role.getHelmet()), Component.text(role.getName() + " Helmet", role.getColor(), TextDecoration.BOLD)));
        addButton(new Button(18, new ItemStack(role.getChestplate()), Component.text(role.getName() + " Chestplate", role.getColor(), TextDecoration.BOLD)));
        addButton(new Button(27, new ItemStack(role.getLeggings()), Component.text(role.getName() + " Leggings", role.getColor(), TextDecoration.BOLD)));
        addButton(new Button(36, new ItemStack(role.getBoots()), Component.text(role.getName() + " Boots", role.getColor(), TextDecoration.BOLD)));

        int slot = 11;
        for (int i = 1; i < 5; i++) {
            RoleBuild activeBuild = builds.getActiveBuilds().get(role.getName());
            if (activeBuild == null) {
                activeBuild = builds.getBuild(role, i).orElseThrow();
                log.warn("setting active build for " + role.getName());
                builds.getActiveBuilds().put(role.getName(), activeBuild);
            }
            final boolean selected = activeBuild.getId() == i;
            Component buildName = Component.text("Build " + i, NamedTextColor.GRAY);
            if (selected) {
                buildName = Component.text("\u00BB Build " + i + " \u00AB", NamedTextColor.GREEN);
            }
            log.info("" + i);
            addButton(new ApplyBuildButton(builds, role, i, slot, getApplyBuildItem(i, selected), buildName));
            addButton(new EditBuildButton(builds, role, i, skillManager, slot + 9));
            addButton(new DeleteBuildButton(builds, role, i, slot + 18));

            slot += 2;
        }

        fillEmpty(Menu.BACKGROUND);
    }

    @NotNull
    private ItemStack getApplyBuildItem(int id, boolean addGlow) {

        ItemStack itemStack;
        switch (id) {
            case 1 -> itemStack = new ItemStack(Material.RED_DYE, 1);
            case 2 -> itemStack = new ItemStack(Material.ORANGE_DYE, 1);
            case 3 -> itemStack = new ItemStack(Material.YELLOW_DYE, 1);
            case 4 -> itemStack = new ItemStack(Material.LIME_DYE, 1);
            default -> throw new IllegalStateException("Unexpected value: " + id);
        }

        return addGlow ? UtilItem.addGlow(itemStack) : itemStack;
    }
}
