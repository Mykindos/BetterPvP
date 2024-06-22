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
    private double baseGrowTime;
    private double growTimeDecreasePerLvl;


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
                "Saplings you plant grow faster taking only <green>" + growTime(level) + "</green> seconds to regrow"
        };
    }

    public long growTime(int level) {
        return (long) (baseGrowTime - (growTimeDecreasePerLvl*level));
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

    @Override
    public void loadConfig() {
        super.loadConfig();
        baseGrowTime = getConfig("baseGrowTime", 90.0, Double.class);
        growTimeDecreasePerLvl = getConfig("growTimeDecreasePerLvl", 2.0, Double.class);
    }
}
