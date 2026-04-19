package me.mykindos.betterpvp.progression.profession.skill.tree;

import java.util.Map;

/**
 * Flat inventory-slot map for a skill tree.
 * Slot index: {@code row * 9 + slotX} (9 slots wide, rows grow downward).
 */
public record SkillTreeLayout(int rowCount, Map<Integer, SkillTreeCell> cells) {

    public SkillTreeCell cellAt(int row, int slotX) {
        return cells.getOrDefault(slotIndex(row, slotX), new SkillTreeCell.Air());
    }

    public static int slotIndex(int row, int slotX) {
        return row * 9 + slotX;
    }
}
