package me.mykindos.betterpvp.progression.profession.skill.fishing.basefishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import org.bukkit.Material;

@Singleton
@NodeId("base_fishing")
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
