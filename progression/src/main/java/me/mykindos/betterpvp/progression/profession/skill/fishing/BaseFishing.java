package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import org.bukkit.Material;

@Singleton
public class BaseFishing extends FishingProgressionSkill  {


    @Inject
    protected BaseFishing(Progression progression) {
        super(progression);

    }

    @Override
    public String getName() {
        return "Base Fishing";
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

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[] { "Thicker Lines", "Feeling Lucky", "Expert Baiter" };
        return new ProgressionSkillDependency(dependencies, 500);
    }
}
