package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

@Singleton
public class PaleOakSaplingSkill extends SaplingSkill {

    @Inject
    public PaleOakSaplingSkill(String name) {
        super(name);
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