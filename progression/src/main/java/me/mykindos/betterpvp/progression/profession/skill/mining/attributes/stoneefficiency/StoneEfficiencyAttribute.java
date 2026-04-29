package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.stoneefficiency;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("stone_efficiency")
public class StoneEfficiencyAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public StoneEfficiencyAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Stone Efficiency";
    }

    @Override
    public String getDescription() {
        return "increased mining speed against stone-based blocks";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getMiningSpeedBonus(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
