package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("fishing_double_treasure_chance")
public class FishingDoubleTreasureChanceAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Double treasure chance";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
