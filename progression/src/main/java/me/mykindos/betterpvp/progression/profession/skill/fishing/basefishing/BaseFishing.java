package me.mykindos.betterpvp.progression.profession.skill.fishing.basefishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import org.bukkit.Material;

@Singleton
@SkillId("base_fishing")
public class BaseFishing extends ProfessionSkill {

    @Inject
    public BaseFishing() {
        super("Base Fishing");
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
