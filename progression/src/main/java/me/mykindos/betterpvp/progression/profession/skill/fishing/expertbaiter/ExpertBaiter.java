package me.mykindos.betterpvp.progression.profession.skill.fishing.expertbaiter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@Singleton
@NodeId("expert_baiter")
public class ExpertBaiter extends ProfessionSkill {

    @Inject
    public ExpertBaiter() {
        super("Expert Baiter");
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Unlocks the crafting recipes for fishing baits."
        };
    }

    public boolean isUnlocked(Player player) {
        return getSkillLevel(player) > 0;
    }

    @Override
    public Material getIcon() {
        return Material.BREAD;
    }
}
