package me.mykindos.betterpvp.progression.profession.skill.fishing.nomoremobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import org.bukkit.Material;

@Singleton
@SkillId("no_more_mobs")
public class NoMoreMobs extends ProfessionSkill {

    @Inject
    private FishingHandler fishingHandler;

    @Inject
    public NoMoreMobs() {
        super("No More Mobs");
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
