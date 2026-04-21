package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("bait_non_consumption_chance")
public class BaitNonConsumptionChanceAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Chance to not consume Baits";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
