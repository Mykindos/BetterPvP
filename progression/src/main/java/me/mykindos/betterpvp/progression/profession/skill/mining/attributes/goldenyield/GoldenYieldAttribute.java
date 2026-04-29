package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.goldenyield;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("golden_yield")
public class GoldenYieldAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public GoldenYieldAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Golden Yield";
    }

    @Override
    public String getDescription() {
        return "increased gold ore yield from Gilded Discovery";
    }

    @Override
    public String getOperation() {
        return "";
    }

    public double getBonusYield(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
