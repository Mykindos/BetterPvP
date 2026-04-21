package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("catch_speed_nearby")
public class CatchSpeedNearbyAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Catch speed (Nearby Players)";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
