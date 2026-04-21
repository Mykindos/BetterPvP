package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes;

import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;

@NodeId("fish_increased_stack_size")
public class FishIncreasedStackSizeAttribute implements IProfessionAttribute {
    @Override
    public String getName() {
        return "Maximum stack size (fish)";
    }

    @Override
    public String getOperation() {
        return "";
    }
}
