package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import org.bukkit.Material;

@Singleton
@NodeId("mangrove_sapling")
public class MangroveSaplingSkill extends SaplingSkill {

    @Inject
    public MangroveSaplingSkill() {
        super("Mangrove Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.MANGROVE_PROPAGULE;
    }

    @Override
    public String getTreeName() {
        return "Mangrove";
    }
}