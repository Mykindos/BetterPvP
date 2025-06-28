package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

@Singleton
public class AcaciaSaplingSkill extends SaplingSkill {

    @Inject
    public AcaciaSaplingSkill(String name) {
        super(name);
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