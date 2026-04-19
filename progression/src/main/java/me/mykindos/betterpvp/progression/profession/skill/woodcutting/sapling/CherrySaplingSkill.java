package me.mykindos.betterpvp.progression.profession.skill.woodcutting.sapling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import org.bukkit.Material;

@Singleton
@SkillId("cherry_sapling")
public class CherrySaplingSkill extends SaplingSkill {

    @Inject
    public CherrySaplingSkill() {
        super("Cherry Sapling");
    }

    @Override
    public Material getSaplingMaterial() {
        return Material.CHERRY_SAPLING;
    }

    @Override
    public String getTreeName() {
        return "Cherry";
    }
}