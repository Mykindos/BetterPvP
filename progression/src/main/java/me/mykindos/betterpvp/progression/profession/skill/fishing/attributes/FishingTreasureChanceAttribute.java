package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("fishing_treasure_chance")
public class FishingTreasureChanceAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Treasure chance";
    }

    @Override
    public String getOperation() {
        return "%";
    }
}
