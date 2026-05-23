package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.fishingdoubletreasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("fishing_double_treasure_chance")
public class FishingDoubleTreasureChanceAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public FishingDoubleTreasureChanceAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Double treasure chance";
    }

    @Override
    public String getDescription() {
        return "chance to double a successful fishing treasure drop";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100;
    }

    @Override
    public String getOperation() {
        return "%";
    }

    public double getChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Fishing", this, profileManager);
    }
}
