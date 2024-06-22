package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;

/**
 * Forest Flourisher is a 2-part skill with its event-handling being in the `clans`
 * plugin
 */
@Singleton
public class ForestFlourisherSkill extends WoodcuttingProgressionSkill {

    @Inject
    public ForestFlourisherSkill(Progression progression) {
        super(progression);
    }

    @Override
    public String getName() {
        return "Forest Flourisher";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Saplings you plant grow faster"
        };
    }

    @Override
    public Material getIcon() {
        return Material.BONE_MEAL;
    }

    public TreeType getTreeType(Block block) {
        return switch (block.getType()) {
            case BIRCH_SAPLING -> TreeType.BIRCH;
            case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
            case ACACIA_SAPLING -> TreeType.ACACIA;
            default -> null;
        };
    }
}
