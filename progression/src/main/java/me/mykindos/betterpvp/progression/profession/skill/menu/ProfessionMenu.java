package me.mykindos.betterpvp.progression.profession.skill.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractScrollGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.menu.button.NodeButton;
import me.mykindos.betterpvp.progression.profession.skill.menu.button.NodeConnectionButton;
import me.mykindos.betterpvp.progression.profession.skill.menu.button.ProfessionInfoButton;
import me.mykindos.betterpvp.progression.profession.skill.menu.button.ScrollDownButton;
import me.mykindos.betterpvp.progression.profession.skill.menu.button.ScrollUpButton;
import me.mykindos.betterpvp.progression.profession.skill.tree.ConnectionType;
import me.mykindos.betterpvp.progression.profession.skill.tree.NodeSlotType;
import me.mykindos.betterpvp.progression.profession.skill.tree.SkillTreeCell;
import me.mykindos.betterpvp.progression.profession.skill.tree.SkillTreeLayout;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ProfessionMenu extends AbstractScrollGui<Item> implements Windowed {

    protected final String profession;
    protected final ProfessionHandler professionHandler;
    protected final ProfessionProfile professionProfile;
    protected final ProfessionNodeManager progressionSkillManager;

    protected final ProfessionData professionData;
    protected static final Item AIR = new SimpleItem(new ItemBuilder(Material.AIR));

    public ProfessionMenu(String profession, ProfessionHandler professionHandler, ProfessionProfile professionProfile, ProfessionNodeManager progressionSkillManager) {
        super(9, 6, false, new Structure(
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "i # # # # # # u d")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', AIR)
                .addIngredient('u', new ScrollUpButton())
                .addIngredient('d', new ScrollDownButton())
                .addIngredient('i', new ProfessionInfoButton(profession, professionProfile.getProfessionDataMap().computeIfAbsent(profession,
                        key -> new ProfessionData(professionProfile.getGamerUUID(), profession)))));
        this.profession = profession;
        this.professionHandler = professionHandler;
        this.professionProfile = professionProfile;
        this.progressionSkillManager = progressionSkillManager;

        professionData = professionProfile.getProfessionDataMap().computeIfAbsent(profession,
                key -> new ProfessionData(professionProfile.getGamerUUID(), profession));


        // Initialize the content with an empty list
        setContent(loadSkillTree());

        // Add scroll handler to update content when scrolled
        addScrollHandler((oldLine, newLine) -> updateContent());
    }

    private List<Item> loadSkillTree() {
        List<Item> items = new ArrayList<>();
        SkillTreeLayout layout = professionHandler.getSkillTree();
        if (layout == null) return items;

        for (int row = 0; row < layout.rowCount(); row++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                final SkillTreeCell cell = layout.cellAt(row, slotX);
                items.add(cellToItem(cell));
            }
        }

        return items;
    }

    private Item cellToItem(SkillTreeCell cell) {
        return switch (cell) {
            case SkillTreeCell.Air ignored -> AIR;
            case SkillTreeCell.Skill s -> getSkillItem(s.skillId());
            case SkillTreeCell.Connection c -> getConnectionItem(c.type(), c.linkedSkillIds().toArray(new String[0]));
        };
    }


    /**
     * Gets the item for a skill
     * This method should be called by specific profession menu implementations to add their skill items.
     *
     * @param nodeName The progression skill to add
     * @return The item that was added
     */
    protected Item getSkillItem(String nodeName) {
        if (nodeName == null) {
            return AIR;
        }

        ProfessionNode node = progressionSkillManager.getSkill(nodeName).orElse(null);
        if(node == null) {
            return AIR;
        }

        return new NodeButton(node, getNodeSlotType(), professionData, progressionSkillManager);
    }

    protected Item getConnectionItem(ConnectionType connectionType, String... nodeNames) {
        if (nodeNames == null || nodeNames.length == 0) {
            return AIR;
        }

        List<ProfessionNode> nodes = new ArrayList<>();
        for (String nodeName : nodeNames) {
            if (nodeName == null) {
                return AIR;
            }

            ProfessionNode node = progressionSkillManager.getSkill(nodeName).orElse(null);
            if (node == null) {
                return AIR;
            }
            nodes.add(node);
        }

        return new NodeConnectionButton(connectionType, nodes, professionData);
    }


    public abstract NodeSlotType getNodeSlotType();

    /**
     * Updates the content of the profession menu.
     * This method should be called after adding all skill items to update the GUI.
     */
    protected void updateContent() {
        bake();
    }

    @Override
    public void bake() {
        ArrayList<SlotElement> elements = new ArrayList<>(content.size());
        for (Item item : content) {
            elements.add(new SlotElement.ItemSlotElement(item));
        }

        this.elements = elements;
        update();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-24><glyph:l_skilltree_gui_" + profession.toLowerCase() + ">", NamedTextColor.WHITE);
    }

}
