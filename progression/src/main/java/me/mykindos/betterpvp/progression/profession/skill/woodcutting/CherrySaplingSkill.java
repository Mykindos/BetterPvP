package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

@Singleton
public class CherrySaplingSkill extends SaplingSkill {

    @Inject
    public CherrySaplingSkill(String name) {
        super(name);
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