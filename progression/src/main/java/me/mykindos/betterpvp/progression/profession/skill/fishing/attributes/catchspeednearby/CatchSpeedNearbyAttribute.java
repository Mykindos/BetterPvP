package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.catchspeednearby;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("catch_speed_nearby")
public class CatchSpeedNearbyAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public CatchSpeedNearbyAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Catch speed (Nearby Players)";
    }

    @Override
    public String getDescription() {
        return "increased catch speed for nearby players";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getCatchSpeedNearbyBonus(Player player) {
        return IProfessionAttribute.computeValue(player, "Fishing", this, profileManager);
    }
}
