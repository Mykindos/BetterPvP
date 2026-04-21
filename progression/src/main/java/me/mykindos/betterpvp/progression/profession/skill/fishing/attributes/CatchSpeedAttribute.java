package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("catch_speed")
public class CatchSpeedAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Catch speed";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
