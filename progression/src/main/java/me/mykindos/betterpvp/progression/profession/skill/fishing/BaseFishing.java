package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Material;

@Singleton
public class BaseFishing extends ProfessionSkillNode {


    @Inject
    public BaseFishing(String name) {
        super(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Allows you to catch fish inside your clan's territory."
        };
    }

    @Override
    public Material getIcon() {
        return Material.GRASS_BLOCK;
    }



}
