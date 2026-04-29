package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.fortune;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("fortune")
public class FortuneAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public FortuneAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Fortune";
    }

    @Override
    public String getDescription() {
        return "chance for ore drops to be doubled";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
