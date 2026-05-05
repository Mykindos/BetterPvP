package me.mykindos.betterpvp.progression.profession.skill.fishing.aggressiverodder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import org.bukkit.Material;

@Singleton
@NodeId("aggressive_rodder")
public class AggressiveRodder extends ProfessionSkill {
    
    @Inject
    public AggressiveRodder() {
        super("Aggressive Rodder");
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
