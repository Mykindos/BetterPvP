package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.baitduration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("bait_duration")
public class BaitDurationAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public BaitDurationAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Bait duration";
    }

    @Override
    public String getDescription() {
        return "increased bait duration";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getBaitDurationBonus(Player player) {
        return IProfessionAttribute.computeValue(player, "Fishing", this, profileManager);
    }
}
