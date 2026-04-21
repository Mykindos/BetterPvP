package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import org.bukkit.Material;

@Singleton
@NodeId("pale_oak_sapling")
public class PaleOakSaplingSkill extends SaplingSkill {

    @Inject
    public PaleOakSaplingSkill() {
        super("Pale Oak Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.PALE_OAK_SAPLING;
    }

    @Override
    public String getTreeName() {
        return "Pale Oak";
    }
}