package me.mykindos.betterpvp.clans.champions.builds.menus;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.builds.menus.buttons.SkillButton;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.SkillManager;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.menu.InjectableMenu;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillMenu extends Menu implements IRefreshingMenu, InjectableMenu {

    @Inject
    private SkillManager skillManager;

    private final RoleBuild roleBuild;

    public SkillMenu(Player player, RoleBuild roleBuild) {
        super(player, 54, "Skill Page");
        this.roleBuild = roleBuild;

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

        for (Skill skill : skillManager.getSkillsForRole(roleBuild.getRole())) {
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
            if (roleBuild.getBuildSkill(skill.getType()) != null) {
                addButton(buildButton(skill, slotNumber, roleBuild.getBuildSkill(skill.getType()).getLevel()));
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

    private void addDefaultButtons(){
        addButton(new Button(0, new ItemStack(Material.IRON_SWORD), ChatColor.GREEN.toString() + ChatColor.BOLD + "Sword Skills"));
        addButton(new Button(9, new ItemStack(Material.IRON_AXE), ChatColor.GREEN.toString() + ChatColor.BOLD + "Axe Skills"));
        addButton(new Button(18, new ItemStack(Material.BOW), ChatColor.GREEN.toString() + ChatColor.BOLD + "Bow Skills"));
        addButton(new Button(27, new ItemStack(Material.RED_DYE, 1), ChatColor.GREEN.toString() + ChatColor.BOLD + "Class Passive A Skills"));
        addButton(new Button(36, new ItemStack(Material.ORANGE_DYE, 1), ChatColor.GREEN.toString() + ChatColor.BOLD + "Class Passive B Skills"));
        addButton(new Button(45, new ItemStack(Material.YELLOW_DYE, 1), ChatColor.GREEN.toString() + ChatColor.BOLD + "Global Passive Skills"));
        addButton(new Button(8, new ItemStack(Material.EMERALD, roleBuild.getPoints()), ChatColor.GREEN.toString() + ChatColor.BOLD + "Skill Points"));
    }

    @Override
    public void postInjection() {
        refresh();
    }
}
