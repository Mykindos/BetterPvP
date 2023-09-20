package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.SkillButton;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillMenu extends Menu implements IRefreshingMenu {

    private final Role role;

    private final SkillManager skillManager;
    @Getter
    private final RoleBuild roleBuild;

    public SkillMenu(Player player, GamerBuilds builds, Role role, int buildNumber, SkillManager skillManager) {
        super(player, 54, Component.text("Skill Page", NamedTextColor.GREEN));
        this.role = role;
        this.skillManager = skillManager;

        roleBuild = builds.getBuilds().stream().filter(build -> build.getRole() == role && build.getId() == buildNumber)
                .findFirst().orElseThrow();
        refresh();
    }

    @Override
    public void refresh() {
        int slotNumber = 0;
        int swordSlotNumber = 1;
        int axeSlotNumber = 10;
        int bowSlotNumber = 19;
        int passiveASlotNumber = 28;
        int passiveBSlotNumber = 37;
        int globalSlotNumber = 46;

        addDefaultButtons();

        for (Skill skill : skillManager.getSkillsForRole(role)) {
            if (skill == null) continue;
            if (skill.getType() == null) continue;
            if (skill.getType() == SkillType.SWORD) {
                slotNumber = swordSlotNumber;
                swordSlotNumber++;
            } else if (skill.getType() == SkillType.AXE) {
                slotNumber = axeSlotNumber;
                axeSlotNumber++;
            } else if (skill.getType() == SkillType.BOW) {
                slotNumber = bowSlotNumber;
                bowSlotNumber++;
            } else if (skill.getType() == SkillType.PASSIVE_A) {
                slotNumber = passiveASlotNumber;
                passiveASlotNumber++;
            } else if (skill.getType() == SkillType.PASSIVE_B) {
                slotNumber = passiveBSlotNumber;
                passiveBSlotNumber++;
            } else if (skill.getType() == SkillType.GLOBAL) {
                slotNumber = globalSlotNumber;
                globalSlotNumber++;
            }


            BuildSkill buildSkill = roleBuild.getBuildSkill(skill.getType());
            if (buildSkill != null) {
                addButton(buildButton(skill, slotNumber, buildSkill.getLevel()));
            } else {
                addButton(buildButton(skill, slotNumber, 1));
            }
        }

    }

    public SkillButton buildButton(Skill skill, int slot, int level) {
        String[] lore;
        lore = skill.getDescription(level);

        List<String> tempLore = new ArrayList<>();
        for (String str : lore) {
            tempLore.add(ChatColor.GRAY + str);
        }
        ItemStack book = null;
        String name = "";
        if (isSkillActive(skill)) {
            book = new ItemStack(Material.ENCHANTED_BOOK);
            name = ChatColor.GREEN.toString() + ChatColor.BOLD + skill.getName() + " (" + level + " / " + skill.getMaxLevel() + ")";
        } else {
            book = new ItemStack(Material.BOOK);
            name = ChatColor.RED + skill.getName();
        }


        return new SkillButton(skill, roleBuild, slot, book, name, tempLore);
    }

    public boolean isSkillActive(Skill skill) {
        for (Skill z : roleBuild.getActiveSkills()) {
            if (z == null) continue;
            if (z.equals(skill)) {
                return true;
            }
        }

        return false;
    }

    private void addDefaultButtons() {
        addButton(new Button(0, new ItemStack(Material.IRON_SWORD), ChatColor.GREEN.toString() + ChatColor.BOLD + "Sword Skills"));
        addButton(new Button(9, new ItemStack(Material.IRON_AXE), ChatColor.GREEN.toString() + ChatColor.BOLD + "Axe Skills"));
        addButton(new Button(18, new ItemStack(Material.BOW), ChatColor.GREEN.toString() + ChatColor.BOLD + "Bow Skills"));
        addButton(new Button(27, new ItemStack(Material.RED_DYE, 1), ChatColor.GREEN.toString() + ChatColor.BOLD + "Class Passive A Skills"));
        addButton(new Button(36, new ItemStack(Material.ORANGE_DYE, 1), ChatColor.GREEN.toString() + ChatColor.BOLD + "Class Passive B Skills"));
        addButton(new Button(45, new ItemStack(Material.YELLOW_DYE, 1), ChatColor.GREEN.toString() + ChatColor.BOLD + "Global Passive Skills"));
        addButton(new Button(8, new ItemStack(Material.EMERALD, roleBuild.getPoints()), ChatColor.GREEN.toString() + ChatColor.BOLD + "Skill Points"));
    }

}
