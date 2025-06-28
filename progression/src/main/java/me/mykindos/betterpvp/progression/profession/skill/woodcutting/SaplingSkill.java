package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

/**
 * Base class for all sapling skills that unlock the ability to plant specific tree types
 */
public abstract class SaplingSkill extends ProfessionSkillNode {

    @Inject
    public SaplingSkill(String name) {
        super(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Unlocks the ability to plant " + getTreeName() + " saplings"
        };
    }

    @Override
    public Material getIcon() {
        return getSaplingMaterial();
    }

    @Override
    public ItemFlag getFlag() {
        return null;
    }

    /**
     * Returns the Material type of the sapling this skill unlocks
     * @return The sapling material
     */
    public abstract Material getSaplingMaterial();

    /**
     * Returns the name of the tree type this skill unlocks
     * @return The tree name
     */
    public abstract String getTreeName();
}