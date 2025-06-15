package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

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
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProfessionInfoButton;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProfessionSkillConnectionButton;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProgressionSkillButton;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ScrollDownItem;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ScrollUpItem;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.ConnectionType;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.SkillNodeType;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                .addIngredient('u', new ScrollUpItem())
                .addIngredient('d', new ScrollDownItem())
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

        // Sort row keys numerically (row_1, row_2, etc.)
        List<String> sortedRowKeys = professionHandler.skillTreeLayout.keySet().stream()
                .sorted((a, b) -> {
                    int numA = Integer.parseInt(a.replaceAll("\\D+", ""));
                    int numB = Integer.parseInt(b.replaceAll("\\D+", ""));
                    return Integer.compare(numA, numB);
                })
                .toList();

        for (String rowKey : sortedRowKeys) {
            Map<String, List<String>> row = professionHandler.skillTreeLayout.get(rowKey);

            // Process each column in order (col_1, col_2, col_3)
            for (String colKey : List.of("col_1", "col_2", "col_3")) {
                List<String> column = row.get(colKey);
                if (column != null) {
                    for (String slotDef : column) {
                        items.add(parseSlotDefinition(slotDef));
                    }
                } else {
                    // Add 3 AIR items if column is missing
                    items.addAll(List.of(AIR, AIR, AIR));
                }
            }
        }

        return items;
    }


    private Item parseSlotDefinition(String slotDef) {
        if ("AIR".equals(slotDef) || slotDef.isEmpty()) {
            return AIR;
        }

        if (slotDef.toLowerCase().startsWith("skill:")) {
            String skillName = slotDef.substring(6);
            return getSkillItem(skillName);
        }

        if (slotDef.toLowerCase().startsWith("connection:")) {
            String[] parts = slotDef.substring(11).split(":");
            if (parts.length >= 2) {
                ConnectionType type = ConnectionType.valueOf(parts[0]);
                String[] nodes = Arrays.copyOfRange(parts, 1, parts.length);
                return getConnectionItem(type, nodes);
            }
        }

        return AIR;
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

        return new ProgressionSkillButton(node, getSkillNodeType(), professionData, progressionSkillManager);
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

        return new ProfessionSkillConnectionButton(connectionType, nodes, professionData);
    }


    public abstract SkillNodeType getSkillNodeType();

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
