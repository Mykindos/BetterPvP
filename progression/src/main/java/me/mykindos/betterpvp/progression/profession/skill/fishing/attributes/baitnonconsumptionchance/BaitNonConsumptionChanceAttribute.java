package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.baitnonconsumptionchance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("bait_non_consumption_chance")
public class BaitNonConsumptionChanceAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public BaitNonConsumptionChanceAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Chance to not consume Baits";
    }

    @Override
    public String getDescription() {
        return "chance that thrown bait is returned to your inventory instead of being consumed";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getNonConsumptionChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Fishing", this, profileManager);
    }
}
