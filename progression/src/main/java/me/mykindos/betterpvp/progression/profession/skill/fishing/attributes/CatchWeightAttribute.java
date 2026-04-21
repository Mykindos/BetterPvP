package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("catch_weight")
public class CatchWeightAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Catch size";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
