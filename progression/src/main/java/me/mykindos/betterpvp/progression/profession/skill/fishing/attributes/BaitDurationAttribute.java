package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("bait_duration")
public class BaitDurationAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Bait duration";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
