package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("fishing_durability_reduction")
public class FishingDurabilityReductionAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Reduced durability loss";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
