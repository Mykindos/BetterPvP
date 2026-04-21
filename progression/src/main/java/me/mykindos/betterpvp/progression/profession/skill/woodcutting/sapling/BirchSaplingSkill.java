package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import org.bukkit.Material;

@Singleton
@NodeId("birch_sapling")
public class BirchSaplingSkill extends SaplingSkill {

    @Inject
    public BirchSaplingSkill() {
        super("Birch Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.BIRCH_SAPLING;
    }

    @Override
    public String getTreeName() {
        return "Birch";
    }
}
