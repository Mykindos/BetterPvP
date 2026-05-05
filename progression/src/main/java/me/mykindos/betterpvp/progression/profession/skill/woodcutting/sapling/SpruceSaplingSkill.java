package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import org.bukkit.Material;

@Singleton
@NodeId("spruce_sapling")
public class SpruceSaplingSkill extends SaplingSkill {

    @Inject
    public SpruceSaplingSkill() {
        super("Spruce Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.SPRUCE_SAPLING;
    }

    @Override
    public String getTreeName() {
        return "Spruce";
    }
}