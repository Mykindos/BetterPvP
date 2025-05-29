package me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ConnectionType {

    STRAIGHT_HORIZONTAL(400),
    STRAIGHT_VERTICAL(401),
    DOWN_LEFT(402),
    DOWN_RIGHT(403),
    UP_LEFT(404),
    UP_RIGHT(405);

    private final int baseModelData;
    
    /**
     * Get the inactive model data for this connection type with the specified skill node type
     */
    public int getInactiveModelData() {
        return baseModelData;
    }
    
    /**
     * Get the active model data for this connection type with the specified skill node type
     */
    public int getActiveModelData(SkillNodeType skillNodeType) {
        return switch (skillNodeType) {
            case RED -> baseModelData + 6;      // 406, 407, 408, 409, 410, 411
            case BLUE -> baseModelData + 106;   // 506, 507, 508, 509, 510, 511
            case GREEN -> baseModelData + 206;  // 606, 607, 608, 609, 610, 611
            case YELLOW -> baseModelData + 306; // 706, 707, 708, 709, 710, 711
        };
    }
}