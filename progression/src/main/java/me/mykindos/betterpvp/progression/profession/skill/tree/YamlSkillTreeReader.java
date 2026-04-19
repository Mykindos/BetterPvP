package me.mykindos.betterpvp.progression.profession.skill.tree;

import lombok.CustomLog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the existing {@code skill_tree.yml} format verbatim — no migration needed.
 * Format: {@code skill_tree.layout.row_N.col_X} → list of 3 slot strings per column.
 */
@CustomLog
public class YamlSkillTreeReader implements SkillTreeReader {

    @Override
    public SkillTreeLayout read(File file) throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection layout = config.getConfigurationSection("skill_tree.layout");
        if (layout == null) {
            throw new IllegalArgumentException("Missing 'skill_tree.layout' in: " + file.getName());
        }

        Map<Integer, SkillTreeCell> cells = new LinkedHashMap<>();
        int maxRow = 0;

        for (String rowKey : layout.getKeys(false)) {
            int rowIndex = Integer.parseInt(rowKey.replace("row_", "")) - 1; // 0-indexed
            maxRow = Math.max(maxRow, rowIndex);

            ConfigurationSection row = layout.getConfigurationSection(rowKey);
            if (row == null) continue;

            for (int col = 0; col < 3; col++) {
                List<String> slots = row.getStringList("col_" + (col + 1));
                for (int sub = 0; sub < Math.min(3, slots.size()); sub++) {
                    int slotX = col * 3 + sub;
                    cells.put(SkillTreeLayout.slotIndex(rowIndex, slotX), parseCell(slots.get(sub)));
                }
            }
        }

        log.info("Loaded YAML skill tree: {} occupied cells, {} rows", cells.size(), maxRow + 1).submit();
        return new SkillTreeLayout(maxRow + 1, cells);
    }

    private SkillTreeCell parseCell(String value) {
        if (value == null || value.isBlank() || value.equals("AIR")) {
            return new SkillTreeCell.Air();
        }
        if (value.startsWith("SKILL:")) {
            return new SkillTreeCell.Skill(value.substring(6));
        }
        if (value.startsWith("CONNECTION:")) {
            // FORMAT: CONNECTION:<TYPE>:<skillId1>[:<skillId2>...]
            String[] parts = value.split(":", -1);
            if (parts.length < 3) return new SkillTreeCell.Air();
            ConnectionType type = ConnectionType.valueOf(parts[1]);
            List<String> linked = Arrays.asList(Arrays.copyOfRange(parts, 2, parts.length));
            return new SkillTreeCell.Connection(type, linked);
        }
        return new SkillTreeCell.Air();
    }
}
