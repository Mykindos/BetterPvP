package me.mykindos.betterpvp.progression.profession.skill.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeDependency;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.menu.ProfessionMenu;
import me.mykindos.betterpvp.progression.profession.skill.tree.NodeSlotType;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NodeButton extends ControlItem<ProfessionMenu> {

    private final ProfessionNode progressionNode;
    private final NodeSlotType skillNodeType;
    private final ProfessionData professionData;
    private final ProfessionNodeManager progressionSkillManager;
    private final ProfessionHandler professionHandler;

    public NodeButton(ProfessionNode progressionNode, NodeSlotType skillNodeType, ProfessionData professionData, ProfessionNodeManager progressionSkillManager, ProfessionHandler professionHandler) {
        this.progressionNode = progressionNode;
        this.skillNodeType = skillNodeType;
        this.professionData = professionData;
        this.progressionSkillManager = progressionSkillManager;
        this.professionHandler = professionHandler;
    }

    @Override
    public ItemProvider getItemProvider(ProfessionMenu professionMenu) {
        int levelsApplied = professionData.getBuild().getSkillLevel(progressionNode);
        Component name = UtilMessage.deserialize("%s%s <white>- %d / %d", doesMeetRequirements() ? "<green>" : "<red>",
                progressionNode.getDisplayName(), levelsApplied, progressionNode.getMaxLevel());

        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder().material(Material.YELLOW_DYE)
                .itemModel(Key.key("minecraft", "l_skilltree_node" + getModelData(levelsApplied)))
                .displayName(name);
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

        int levelsAvailable = professionHandler.getAvailableSkillPoints(professionData);

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
                professionData.getBuild().getNodes().put(progressionNode, professionData.getBuild().getSkillLevel(progressionNode) + Math.min(levelsAvailable, 5));
                SoundEffect.HIGH_PITCH_PLING.play(player);
            }
        } else {
            // Add 1 to skill level
            professionData.getBuild().getNodes().put(progressionNode, professionData.getBuild().getSkillLevel(progressionNode) + 1);
            SoundEffect.HIGH_PITCH_PLING.play(player);
        }

        getGui().updateControlItems();

    }

    private int getModelData(int levelsApplied) {
        if (!doesMeetRequirements()) {
            return 413;
        }

        if (levelsApplied == progressionNode.getMaxLevel()) {
            return skillNodeType.getCompletedModelData();
        }

        if (levelsApplied > 0) {
            return skillNodeType.getStartedModelData();
        }

        return 412;
    }

    private boolean doesMeetRequirements() {
        ProfessionNodeDependency dependencies = progressionNode.getDependencies();
        int professionLevel = professionData.getLevelFromExperience(professionData.getExperience());

        if (dependencies == null) {
            return true;
        }

        if (!dependencies.getNodes().isEmpty()) {
            int totalLevels = 0;
            for (String dependency : dependencies.getNodes()) {
                Optional<ProfessionNode> dependencySkillOptional = progressionSkillManager.getSkill(dependency);
                if (dependencySkillOptional.isEmpty()) {
                    return false;
                }

                ProfessionNode dependencySkill = dependencySkillOptional.get();
                int dependencyLevel = professionData.getBuild().getSkillLevel(dependencySkill);
                if (dependencyLevel < dependencySkill.getMaxLevel()) {
                    return false;
                }

                totalLevels += dependencyLevel;
            }

            return totalLevels >= dependencies.getLevelsRequired()
                    && dependencies.getRequiredLevel() <= professionLevel;
        }


        return dependencies.getRequiredLevel() <= professionLevel;
    }
}
