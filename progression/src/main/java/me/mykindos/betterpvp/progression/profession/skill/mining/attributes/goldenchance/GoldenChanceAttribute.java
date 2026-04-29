package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.goldenchance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("golden_chance")
public class GoldenChanceAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public GoldenChanceAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Golden Chance";
    }

    @Override
    public String getDescription() {
        return "increased Gilded Discovery trigger chance";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getBonusChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
