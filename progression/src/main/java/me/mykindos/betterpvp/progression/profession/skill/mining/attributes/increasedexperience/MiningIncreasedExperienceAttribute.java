package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.increasedexperience;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("mining_increased_experience")
public class MiningIncreasedExperienceAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public MiningIncreasedExperienceAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Increased experience";
    }

    @Override
    public String getDescription() {
        return "mining experience gained from all sources";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getExperienceIncrease(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
