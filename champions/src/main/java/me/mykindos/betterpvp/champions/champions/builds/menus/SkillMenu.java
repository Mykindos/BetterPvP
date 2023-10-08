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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillMenu extends Menu implements IRefreshingMenu {

    /**
     * The tag resolver for skill descriptions.
     * It will parse all tags with the name 'val'
     */
    public static final TagResolver TAG_RESOLVER = TagResolver.resolver(
            TagResolver.resolver("val", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("effect", Tag.styling(NamedTextColor.WHITE)),
            TagResolver.resolver("stat", Tag.styling(NamedTextColor.YELLOW))
    );

    private final Role role;

    private final SkillManager skillManager;
    @Getter
    private final RoleBuild roleBuild;

    public SkillMenu(Player player, GamerBuilds builds, Role role, int buildNumber, SkillManager skillManager) {
        super(player, 54, Component.text("Edit Build"));
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
            if (!skill.isEnabled()) continue;
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

        fillEmpty(Menu.BACKGROUND);
    }

    public SkillButton buildButton(Skill skill, int slot, int level) {
        String[] lore = skill.getDescription(level);

        List<Component> tempLore = new ArrayList<>();
        for (String str : lore) {
            Component component = MiniMessage.miniMessage().deserialize("<gray>" + str, TAG_RESOLVER);
            tempLore.add(component);
        }

        ItemStack book;
        Component name;
        if (isSkillActive(skill)) {
            book = new ItemStack(Material.WRITTEN_BOOK, level);
            name = Component.text(skill.getName() + " (" + level + " / " + skill.getMaxLevel() + ")", NamedTextColor.GREEN, TextDecoration.BOLD);
        } else {
            book = new ItemStack(Material.BOOK);
            name = Component.text(skill.getName(), NamedTextColor.RED);
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
        addButton(new Button(0, new ItemStack(Material.IRON_SWORD), Component.text("Sword Skills", NamedTextColor.GREEN, TextDecoration.BOLD)));
        addButton(new Button(9, new ItemStack(Material.IRON_AXE), Component.text("Axe Skills", NamedTextColor.GREEN, TextDecoration.BOLD)));
        addButton(new Button(18, new ItemStack(Material.BOW), Component.text("Bow Skills", NamedTextColor.GREEN, TextDecoration.BOLD)));
        addButton(new Button(27, new ItemStack(Material.RED_DYE), Component.text("Class Passive A Skills", NamedTextColor.GREEN, TextDecoration.BOLD)));
        addButton(new Button(36, new ItemStack(Material.ORANGE_DYE), Component.text("Class Passive B Skills", NamedTextColor.GREEN, TextDecoration.BOLD)));
        addButton(new Button(45, new ItemStack(Material.YELLOW_DYE), Component.text("Global Passive Skills", NamedTextColor.GREEN, TextDecoration.BOLD)));
        addButton(new Button(8, new ItemStack(Material.GOLD_INGOT, roleBuild.getPoints()), Component.text(roleBuild.getPoints() + " Skill Points Remaining", NamedTextColor.GREEN, TextDecoration.BOLD)));
    }

}
