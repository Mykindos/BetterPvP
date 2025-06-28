package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

@Singleton
public class JungleSaplingSkill extends SaplingSkill {

    @Inject
    public JungleSaplingSkill(String name) {
        super(name);
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