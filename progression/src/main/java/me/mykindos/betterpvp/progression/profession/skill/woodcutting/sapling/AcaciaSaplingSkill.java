package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import org.bukkit.Material;

@Singleton
@SkillId("acacia_sapling")
public class AcaciaSaplingSkill extends SaplingSkill {

    @Inject
    public AcaciaSaplingSkill() {
        super("Acacia Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.ACACIA_SAPLING;
    }

    @Override
    public String getTreeName() {
        return "Acacia";
    }
}