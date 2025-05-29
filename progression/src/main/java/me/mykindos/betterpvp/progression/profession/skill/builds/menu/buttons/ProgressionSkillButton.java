package me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeDependency;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.ProfessionMenu;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.SkillNodeType;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ProgressionSkillButton extends ControlItem<ProfessionMenu> {

    private final ProfessionNode progressionNode;
    private final SkillNodeType skillNodeType;
    private final ProfessionData professionData;
    private final ProfessionNodeManager progressionSkillManager;

    public ProgressionSkillButton(ProfessionNode progressionNode, SkillNodeType skillNodeType, ProfessionData professionData, ProfessionNodeManager progressionSkillManager) {
        this.progressionNode = progressionNode;
        this.skillNodeType = skillNodeType;
        this.professionData = professionData;
        this.progressionSkillManager = progressionSkillManager;
    }

    @Override
    public ItemProvider getItemProvider(ProfessionMenu professionMenu) {
        int levelsApplied = professionData.getBuild().getSkillLevel(progressionNode);
        Component name = UtilMessage.deserialize("%s%s <white>- %d / %d", doesMeetRequirements() ? "<green>" : "<red>",
                progressionNode.getDisplayName(), levelsApplied, progressionNode.getMaxLevel());

        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder().material(Material.YELLOW_DYE).customModelData(doesMeetRequirements() ? levelsApplied == progressionNode.getMaxLevel()
                ? skillNodeType.getCompletedModelData() : levelsApplied > 0 ? skillNodeType.getStartedModelData() : 412 : 413).displayName(name);
        Optional.ofNullable(progressionNode.getFlag()).ifPresent(itemViewBuilder::flag);
        itemViewBuilder.glow(progressionNode.isGlowing());

        if (progressionNode.getDescription(levelsApplied) != null) {
            for (String description : progressionNode.getDescription(levelsApplied)) {
                itemViewBuilder.lore(UtilMessage.deserialize(description));
            }
        }

        if (!doesMeetRequirements()) {
            ProfessionNodeDependency dependencies = progressionNode.getDependencies();
            if (dependencies != null) {
                itemViewBuilder.lore(Component.text(""));


                if(dependencies.getRequiredLevel() > 0) {
                    itemViewBuilder.lore(UtilMessage.deserialize("<white>Required Level: <red>%d", dependencies.getRequiredLevel()));
                    itemViewBuilder.lore(Component.text(""));
                }

                if(!dependencies.getNodes().isEmpty()) {
                    itemViewBuilder.lore(UtilMessage.deserialize("<red>You must unlock a previous node first."));

                }
            }
        } else {
            if (levelsApplied < progressionNode.getMaxLevel()) {
                itemViewBuilder.action(ClickActions.LEFT, Component.text("Increase Level"));

                if (progressionNode.getMaxLevel() - levelsApplied >= 5) {
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

        if (professionData.getBuild().getSkillLevel(progressionNode) >= progressionNode.getMaxLevel()) {
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
            int levelsApplied = professionData.getBuild().getSkillLevel(progressionNode);
            if (progressionNode.getMaxLevel() - levelsApplied >= 5) {
                professionData.getBuild().getSkills().put(progressionNode, professionData.getBuild().getSkillLevel(progressionNode) + Math.min(levelsAvailable, 5));
                SoundEffect.HIGH_PITCH_PLING.play(player);
            }
        } else {
            // Add 1 to skill level
            professionData.getBuild().getSkills().put(progressionNode, professionData.getBuild().getSkillLevel(progressionNode) + 1);
            SoundEffect.HIGH_PITCH_PLING.play(player);
        }

        getGui().updateControlItems();

    }

    private boolean doesMeetRequirements() {

        if (progressionNode.getDependencies() != null && !progressionNode.getDependencies().getNodes().isEmpty()) {
            int totalLevels = 0;
            for (String dependency : progressionNode.getDependencies().getNodes()) {
                Optional<ProfessionNode> dependencySkillOptional = progressionSkillManager.getSkill(dependency);
                if (dependencySkillOptional.isPresent()) {
                    totalLevels += professionData.getBuild().getSkillLevel(dependencySkillOptional.get());

                }
            }

            return totalLevels >= progressionNode.getDependencies().getLevelsRequired()
                    && progressionNode.getDependencies().getRequiredLevel() < professionData.getLevelFromExperience(professionData.getExperience());
        }


        return progressionNode.getDependencies().getRequiredLevel() < professionData.getLevelFromExperience(professionData.getExperience());
    }
}
