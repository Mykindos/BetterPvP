package me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.ProfessionMenu;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ProgressionSkillButton extends ControlItem<ProfessionMenu> {

    private final ProgressionSkill progressionSkill;
    private final ProfessionData professionData;
    private final ProgressionSkillManager progressionSkillManager;

    public ProgressionSkillButton(ProgressionSkill progressionSkill, ProfessionData professionData, ProgressionSkillManager progressionSkillManager) {
        this.progressionSkill = progressionSkill;
        this.professionData = professionData;
        this.progressionSkillManager = progressionSkillManager;
    }

    @Override
    public ItemProvider getItemProvider(ProfessionMenu professionMenu) {
        int levelsApplied = professionData.getBuild().getSkillLevel(progressionSkill);
        Component name = UtilMessage.deserialize("%s%s <white>- %d / %d", doesMeetRequirements() ? "<green>" : "<red>",
                progressionSkill.getName(), levelsApplied, progressionSkill.getMaxLevel());

        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder().material(progressionSkill.getIcon()).displayName(name);
        Optional.ofNullable(progressionSkill.getFlag()).ifPresent(itemViewBuilder::flag);

        if (progressionSkill.getDescription(levelsApplied) != null) {
            for (String description : progressionSkill.getDescription(levelsApplied)) {
                itemViewBuilder.lore(UtilMessage.deserialize(description));
            }
        }

        if (!doesMeetRequirements()) {
            ProgressionSkillDependency dependencies = progressionSkill.getDependencies();
            if (dependencies != null) {
                itemViewBuilder.lore(Component.text(""));
                itemViewBuilder.lore(UtilMessage.deserialize("<red>To unlock this skill, you must spend at least"));
                itemViewBuilder.lore(UtilMessage.deserialize("<green>%d <red>points across the following skills:", dependencies.getLevelsAssigned()));

                for (String dependency : dependencies.getDependencies()) {
                    itemViewBuilder.lore(Component.text(" - ", NamedTextColor.GRAY).append(Component.text(dependency, NamedTextColor.WHITE)));
                }
            }
        } else {
            if (levelsApplied < progressionSkill.getMaxLevel()) {
                itemViewBuilder.action(ClickActions.LEFT, Component.text("Increase Level"));

                if (progressionSkill.getMaxLevel() - levelsApplied >= 5) {
                    itemViewBuilder.action(ClickActions.LEFT_SHIFT, Component.text("Increase Level by 5"));
                }
            }

        }

        return itemViewBuilder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (!clickType.isLeftClick()) return;

        final int currentLevel = professionData.getLevelFromExperience(professionData.getExperience());
        final int totalSkillLevels = professionData.getBuild().getSkills().values().stream().mapToInt(Integer::intValue).sum();

        int levelsAvailable = currentLevel - totalSkillLevels;

        if (levelsAvailable <= 0) {
            UtilMessage.message(player, "Profession", "You do not have any skill points available!");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return;
        }

        if (professionData.getBuild().getSkillLevel(progressionSkill) >= progressionSkill.getMaxLevel()) {
            UtilMessage.message(player, "Profession", "This skill is already the max level");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return;
        }

        if (!doesMeetRequirements()) {
            UtilMessage.message(player, "Profession", "You do not meet the requirements to unlock this skill!");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return;
        }

        if (clickType.isShiftClick()) {
            // Add 5 to skill level
            int levelsApplied = professionData.getBuild().getSkillLevel(progressionSkill);
            if (progressionSkill.getMaxLevel() - levelsApplied >= 5) {
                professionData.getBuild().getSkills().put(progressionSkill, professionData.getBuild().getSkillLevel(progressionSkill) + Math.min(levelsAvailable, 5));
                SoundEffect.HIGH_PITCH_PLING.play(player);
            }
        } else {
            // Add 1 to skill level
            professionData.getBuild().getSkills().put(progressionSkill, professionData.getBuild().getSkillLevel(progressionSkill) + 1);
            SoundEffect.HIGH_PITCH_PLING.play(player);
        }

        getGui().updateControlItems();

    }

    private boolean doesMeetRequirements() {

        if (progressionSkill.getDependencies() != null) {
            int totalLevels = 0;
            for (String dependency : progressionSkill.getDependencies().getDependencies()) {
                Optional<ProgressionSkill> dependencySkillOptional = progressionSkillManager.getSkill(dependency);
                if (dependencySkillOptional.isPresent()) {
                    totalLevels += professionData.getBuild().getSkillLevel(dependencySkillOptional.get());

                }
            }

            return totalLevels >= progressionSkill.getDependencies().getLevelsAssigned();
        }


        return true;
    }
}
