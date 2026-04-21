package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("bait_multiplier")
public class BaitMultiplierAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Bait multiplier";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
