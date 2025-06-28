package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

@Singleton
public class SpruceSaplingSkill extends SaplingSkill {

    @Inject
    public SpruceSaplingSkill(String name) {
        super(name);
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