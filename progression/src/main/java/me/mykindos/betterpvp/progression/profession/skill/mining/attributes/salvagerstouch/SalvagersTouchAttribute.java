package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.salvagerstouch;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("salvagers_touch")
public class SalvagersTouchAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public SalvagersTouchAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Salvager's Touch";
    }

    @Override
    public String getDescription() {
        return "chance to recover an extra materials from stone blocks";
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
