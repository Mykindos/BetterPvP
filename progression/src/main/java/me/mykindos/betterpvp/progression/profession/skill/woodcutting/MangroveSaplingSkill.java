package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

@Singleton
public class MangroveSaplingSkill extends SaplingSkill {

    @Inject
    public MangroveSaplingSkill(String name) {
        super(name);
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