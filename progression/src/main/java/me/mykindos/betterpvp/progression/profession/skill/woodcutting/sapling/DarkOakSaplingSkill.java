package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import org.bukkit.Material;

@Singleton
@SkillId("dark_oak_sapling")
public class DarkOakSaplingSkill extends SaplingSkill {

    @Inject
    public DarkOakSaplingSkill() {
        super("Dark Oak Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.DARK_OAK_SAPLING;
    }

    @Override
    public String getTreeName() {
        return "Dark Oak";
    }
}