package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Material;

@Singleton
public class NoMoreMobs extends ProfessionSkillNode {

    @Inject
    private FishingHandler fishingHandler;

    @Inject
    public NoMoreMobs(String name) {
        super(name);
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "You will no longer catch mobs while fishing."
        };
    }

    @Override
    public Material getIcon() {
        return Material.ZOMBIE_HEAD;
    }

}
