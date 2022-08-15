package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillUpdateEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SkillButton extends Button {

    private final Skill skill;
    private final RoleBuild roleBuild;

    public SkillButton(Skill skill, RoleBuild roleBuild, int slot, ItemStack item, String name, List<String> lore) {
        super(slot, item, name, lore);
        this.skill = skill;
        this.roleBuild = roleBuild;
    }

    @Override
    public void onClick(Player player, ClickType type) {

        if (type == ClickType.LEFT) {
            if (roleBuild.getPoints() > 0) {

                BuildSkill buildSkill = roleBuild.getBuildSkill(skill.getType());
                if (buildSkill == null) {
                    BuildSkill newSkill = new BuildSkill(skill, 1);
                    roleBuild.setSkill(skill.getType(), newSkill);
                    roleBuild.takePoint();
                    UtilServer.callEvent(new SkillEquipEvent(player, newSkill.getSkill(), roleBuild));
                } else {
                    if (buildSkill.getLevel() < skill.getMaxLevel()) {
                        if (buildSkill.getSkill().equals(skill)) {
                            roleBuild.takePoint();
                            roleBuild.setSkill(skill.getType(), skill, buildSkill.getLevel() + 1);
                            UtilServer.callEvent(new SkillUpdateEvent(player, buildSkill.getSkill(), roleBuild));
                        }
                    }

                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
            }
        } else if (type == ClickType.RIGHT) {
            if (roleBuild.getPoints() < 12) {
                if (skill.getType() == null) return;
                BuildSkill buildSkill = roleBuild.getBuildSkill(skill.getType());
                if (buildSkill == null) return;

                roleBuild.setSkill(skill.getType(), new BuildSkill(skill, buildSkill.getLevel() - 1));
                roleBuild.addPoint();
                buildSkill.setLevel(buildSkill.getLevel() - 1);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);

                if (buildSkill.getLevel() == 0) {
                    roleBuild.setSkill(skill.getType(), null);
                    UtilServer.callEvent(new SkillDequipEvent(player, buildSkill.getSkill(), roleBuild));
                    return;
                }

                UtilServer.callEvent(new SkillUpdateEvent(player, buildSkill.getSkill(), roleBuild));


            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
            }
        }
    }
}