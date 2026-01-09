package me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.ProfessionMenu;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.ConnectionType;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProfessionSkillConnectionButton extends ControlItem<ProfessionMenu> {

    private final ConnectionType connectionType;
    private final List<ProfessionNode> targetSkills;
    private final ProfessionData professionData;

    public ProfessionSkillConnectionButton(ConnectionType connectionType, List<ProfessionNode> targetSkills, ProfessionData professionData) {
        this.connectionType = connectionType;
        this.targetSkills = targetSkills;
        this.professionData = professionData;
    }

    // Convenience constructor for single node (backward compatibility)
    public ProfessionSkillConnectionButton(ConnectionType connectionType, ProfessionNode targetSkill, ProfessionData professionData) {
        this.connectionType = connectionType;
        this.targetSkills = List.of(targetSkill);
        this.professionData = professionData;
    }

    @Override
    public ItemProvider getItemProvider(ProfessionMenu professionMenu) {
        // Check if ALL target skills meet the condition (have levels > 0)
        boolean allSkillsActive = targetSkills.stream()
                .allMatch(skill -> professionData.getBuild().getSkillLevel(skill) > 0);

        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder()
                .material(Material.YELLOW_DYE)
                .hideTooltip(true)
                .itemModel(new NamespacedKey("minecraft", "l_skilltree_path" + (allSkillsActive ?
                        connectionType.getActiveModelData(getGui().getSkillNodeType()) :
                        connectionType.getInactiveModelData())));

        return itemViewBuilder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        // Handle click logic if needed
    }
}