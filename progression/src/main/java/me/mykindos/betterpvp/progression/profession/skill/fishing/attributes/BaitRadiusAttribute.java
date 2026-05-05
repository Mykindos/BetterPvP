package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("bait_radius")
public class BaitRadiusAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Bait radius";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
