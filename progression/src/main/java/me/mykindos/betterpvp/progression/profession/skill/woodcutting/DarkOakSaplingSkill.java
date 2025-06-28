package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

@Singleton
public class DarkOakSaplingSkill extends SaplingSkill {

    @Inject
    public DarkOakSaplingSkill(String name) {
        super(name);
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