package me.mykindos.betterpvp.progression.profession.skill.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.locale.Translations;
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
import net.kyori.adventure.text.format.NamedTextColor;
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
        Component name = Translations.component("progression.menu.node.name",
                Component.text(progressionNode.getDisplayName()).color(doesMeetRequirements() ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text(levelsApplied),
                Component.text(progressionNode.getMaxLevel())).color(NamedTextColor.WHITE);

        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder().material(Material.YELLOW_DYE)
                .itemModel(Key.key("minecraft", "l_skilltree_node" + getModelData(levelsApplied)))
                .displayName(name);
        Optional.ofNullable(progressionNode.getFlag()).ifPresent(itemViewBuilder::flag);
        itemViewBuilder.glow(progressionNode.isGlowing());

        if (progressionNode.getDescription(levelsApplied) != null) {
            for (Component description : progressionNode.getDescription(levelsApplied)) {
                // Apply safe default styling similar to Champions lore elsewhere
                itemViewBuilder.lore(description);
            }
        }

        if (!doesMeetRequirements()) {
            ProfessionNodeDependency dependencies = progressionNode.getDependencies();
            if (dependencies != null) {
                itemViewBuilder.lore(Component.text(""));


                if(dependencies.getRequiredLevel() > 0) {
                    itemViewBuilder.lore(Translations.component("progression.menu.node.required-level", Component.text(dependencies.getRequiredLevel()).color(NamedTextColor.RED)).color(NamedTextColor.WHITE));
                    itemViewBuilder.lore(Component.text(""));
                }

                if(!dependencies.getNodes().isEmpty()) {
                    itemViewBuilder.lore(Translations.component("progression.menu.node.locked").color(NamedTextColor.RED));

                }
            }
        } else {
            if (levelsApplied < progressionNode.getMaxLevel()) {
                itemViewBuilder.action(ClickActions.LEFT, Translations.component("progression.menu.node.increase-level"));

                if (progressionNode.getMaxLevel() - levelsApplied >= 5) {
                    itemViewBuilder.action(ClickActions.LEFT_SHIFT, Translations.component("progression.menu.node.increase-level-5"));
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
            UtilMessage.message(player, "core.prefix.profession", "progression.menu.node.no-skill-points");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return;
        }

        if (professionData.getBuild().getSkillLevel(progressionNode) >= progressionNode.getMaxLevel()) {
            UtilMessage.message(player, "core.prefix.profession", "progression.menu.node.already-max-level");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return;
        }

        if (!doesMeetRequirements()) {
            UtilMessage.message(player, "core.prefix.profession", "progression.menu.node.requirements-not-met");
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

        if (dependencies.getRequiredLevel() > professionLevel) {
            return false;
        }

        if (!dependencies.getNodes().isEmpty()) {
            for (String dependency : dependencies.getNodes()) {
                Optional<ProfessionNode> dependencySkillOptional = progressionSkillManager.getSkill(dependency);
                if (dependencySkillOptional.isEmpty()) {
                    continue;
                }

                if (professionData.getBuild().getSkillLevel(dependencySkillOptional.get()) >= 1) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}
