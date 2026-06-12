package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.fishingtreasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("fishing_treasure_chance")
public class FishingTreasureChanceAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public FishingTreasureChanceAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Treasure chance";
    }

    @Override
    public String getDescription() {
        return "chance to find fishing treasure after catching fish";
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
