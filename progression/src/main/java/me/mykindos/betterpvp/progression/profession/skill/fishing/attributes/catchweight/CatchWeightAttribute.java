package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.catchweight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("catch_weight")
public class CatchWeightAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public CatchWeightAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Catch size";
    }

    @Override
    public String getDescription() {
        return "increases the weight of fish caught, granting more items and experience";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getCatchWeightBonus(Player player) {
        return IProfessionAttribute.computeValue(player, "Fishing", this, profileManager);
    }
}
