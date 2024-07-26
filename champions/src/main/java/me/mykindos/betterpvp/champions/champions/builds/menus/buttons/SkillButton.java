package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillUpdateEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class SkillButton extends FlashingButton<SkillMenu> {

    private final Skill skill;
    private final RoleBuild roleBuild;
    private final RoleBuild promptBuild;

    /**
     *
     * @param skill
     * @param roleBuild
     * @param promptBuild The optional rolebuild to prompt the player to create. Null if empty
     */
    public SkillButton(Skill skill, RoleBuild roleBuild, @Nullable RoleBuild promptBuild) {
        this.skill = skill;
        this.roleBuild = roleBuild;
        this.promptBuild = promptBuild;
    }

    @Override
    public ItemProvider getItemProvider(SkillMenu gui) {
        BuildSkill buildSkill = roleBuild.getBuildSkill(skill.getType());
        int level = buildSkill != null && buildSkill.getSkill() == skill ? buildSkill.getLevel() : 0;
        int displayLevel = Math.max(1, level);

        final ItemView.ItemViewBuilder builder = ItemView.builder();
        if (skill.getTags() != null) {
            builder.prelore(skill.getTags());
        }

        builder.lore(Arrays.stream(skill.parseDescription(displayLevel)).toList());

        int desiredLevel = 0;

        //if this is the correct role and build
        if (promptBuild != null && promptBuild.getRole() == roleBuild.getRole()) {
            builder.glow(this.isFlash());
            BuildSkill promptBuildSkill = promptBuild.getBuildSkill(this.skill.getType());
            BuildSkill currentBuildSkill = roleBuild.getBuildSkill(this.skill.getType());
            if (promptBuild.getId() == roleBuild.getId()) {
                //if this is a skill we want
                if (promptBuild.getActiveSkills().contains(this.skill)) {
                    //we have it, set flashing if not correct level
                    if (currentBuildSkill != null &&
                            promptBuildSkill.getSkill().equals(currentBuildSkill.getSkill())) {
                        this.setFlashing(promptBuildSkill.getLevel() != currentBuildSkill.getLevel());
                        desiredLevel = promptBuildSkill.getLevel();
                    } else { //we do not have this skill, but want it
                        this.setFlashing(true);
                        desiredLevel = promptBuildSkill.getLevel();
                    }
                } else { //we don't want this skill, flash if we have it
                    this.setFlashing(currentBuildSkill != null && currentBuildSkill.getSkill().equals(this.skill));
                    desiredLevel = 0;
                }
            }
        }

        boolean active = roleBuild.getActiveSkills().stream().anyMatch(s -> s != null && s.equals(this.skill));
        if (active) {
            Material flashMaterial = this.isFlash() ? Material.WRITTEN_BOOK : Material.BOOK;
            builder.material(this.isFlashing() ? flashMaterial : Material.WRITTEN_BOOK);
            builder.amount(displayLevel);

            Component standardComponent = Component.text(skill.getName() + " (" + displayLevel + " / " + skill.getMaxLevel() + ")", NamedTextColor.GREEN, TextDecoration.BOLD);
            Component flashingComponent = Component.empty().append(Component.text("Click Me!", NamedTextColor.RED).appendSpace())
                                            .append(standardComponent).appendSpace()
                                            .append(Component.text("(" + desiredLevel + ")", NamedTextColor.GOLD));

            builder.displayName(isFlashing() ? flashingComponent : standardComponent);
        } else {
            builder.material(Material.BOOK);
            Component standardComponent = Component.text(skill.getName(), NamedTextColor.RED);
            Component flashingComponent = Component.empty().append(Component.text("Click Me!", NamedTextColor.GREEN).appendSpace())
                    .append(standardComponent).appendSpace()
                    .append(Component.text("(" + desiredLevel + ")", NamedTextColor.GOLD));
            builder.displayName(isFlashing() ? flashingComponent : standardComponent);
        }

        if (displayLevel < skill.getMaxLevel()) {
            builder.action(ClickActions.LEFT, Component.text("Increase Level"));
        }

        if (level > 0) {
            builder.action(ClickActions.RIGHT, Component.text("Decrease Level"));
        }

        return builder.flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP).frameLore(true).build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType == ClickType.DOUBLE_CLICK) return;

        BuildSkill buildSkill = roleBuild.getBuildSkill(skill.getType());

        int currentLevel = buildSkill == null ? 0 : buildSkill.getLevel();
        final boolean isDifferent = buildSkill != null && !buildSkill.getSkill().equals(skill);
        if ((isDifferent || currentLevel < skill.getMaxLevel()) && ClickActions.LEFT.accepts(clickType)) {
            // Return if we don't have enough points
            if (roleBuild.getPoints() <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
                return;
            }

            // Return if skill isn't enabled
            if (!skill.isEnabled()) {
                UtilMessage.simpleMessage(player, "Skills", "This skill is not enabled.");
                UtilSound.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f, false);
                return;
            }

            // Deselect the current skill and select this if we don't have it
            if (isDifferent) {
                // Grant points back and remove the old skill
                roleBuild.setPoints(roleBuild.getPoints() + buildSkill.getLevel());
                roleBuild.setSkill(buildSkill.getSkill().getType(), null);

                // Call events
                UtilServer.callEvent(new SkillDequipEvent(player, buildSkill.getSkill(), roleBuild));

                // Replace
                buildSkill = null;
            }

            // If we don't have the skill, add it
            if (buildSkill == null) {
                BuildSkill newSkill = new BuildSkill(skill, 1);
                roleBuild.setSkill(skill.getType(), newSkill);
                roleBuild.takePoint();
                UtilServer.callEvent(new SkillEquipEvent(player, newSkill.getSkill(), roleBuild));
            } else { // Otherwise, increase the level
                roleBuild.takePoint();
                roleBuild.setSkill(skill.getType(), skill, buildSkill.getLevel() + 1);
                UtilServer.callEvent(new SkillUpdateEvent(player, buildSkill.getSkill(), roleBuild));
            }

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            getGui().updateControlItems();
        } else if (currentLevel > 0 && ClickActions.RIGHT.accepts(clickType)) {
            // Cancel if we're trying to remove a skill we don't have selected
            if (isDifferent) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
                return;
            }

            // Cancel if we'll gain more than 12 points
            if (roleBuild.getPoints() >= 12) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
                return;
            }

            // Run
            roleBuild.setSkill(skill.getType(), new BuildSkill(skill, buildSkill.getLevel() - 1));
            roleBuild.addPoint();
            buildSkill.setLevel(buildSkill.getLevel() - 1);

            // If we have no points, remove the skill
            if (buildSkill.getLevel() == 0) {
                roleBuild.setSkill(skill.getType(), null);
                UtilServer.callEvent(new SkillDequipEvent(player, buildSkill.getSkill(), roleBuild));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
                getGui().updateControlItems();
                return;
            }

            UtilServer.callEvent(new SkillUpdateEvent(player, buildSkill.getSkill(), roleBuild));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            getGui().updateControlItems();
        } else {
            SoundEffect.WRONG_ACTION.play(player);
        }
    }

}