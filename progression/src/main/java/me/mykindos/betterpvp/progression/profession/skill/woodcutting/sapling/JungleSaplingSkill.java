package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import org.bukkit.Material;

@Singleton
@NodeId("jungle_sapling")
public class JungleSaplingSkill extends SaplingSkill {

    @Inject
    public JungleSaplingSkill() {
        super("Jungle Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.JUNGLE_SAPLING;
    }

    @Override
    public String getTreeName() {
        return "Jungle";
    }
}