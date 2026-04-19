package me.mykindos.betterpvp.progression.profession.skill.loader;

import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;

import java.util.List;

public interface NodeLoaderStrategy {
    List<ProfessionNode> loadNodes(Progression progression, String profession);
}
