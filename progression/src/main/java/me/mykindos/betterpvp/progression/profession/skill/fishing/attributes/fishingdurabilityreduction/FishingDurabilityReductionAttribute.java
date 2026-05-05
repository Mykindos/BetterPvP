package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.fishingdurabilityreduction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("fishing_durability_reduction")
public class FishingDurabilityReductionAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public FishingDurabilityReductionAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Reduced durability loss";
    }

    @Override
    public String getDescription() {
        return "reduces the durability consumed by your fishing rod on each cast";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getDurabilityReductionBonus(Player player) {
        return IProfessionAttribute.computeValue(player, "Fishing", this, profileManager);
    }
}
