package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Material;

@Singleton
public class AggressiveRodder extends ProfessionSkillNode {
    
    @Inject
    public AggressiveRodder(String name) {
        super(name);
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your rod will now also pull enemies towards",
                "you if you hook them"
        };
    }

    @Override
    public Material getIcon() {
        return Material.ZOMBIE_HEAD;
    }

}
