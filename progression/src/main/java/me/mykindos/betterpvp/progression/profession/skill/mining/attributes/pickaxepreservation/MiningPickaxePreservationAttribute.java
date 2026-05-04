package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.pickaxepreservation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("pickaxe_preservation")
public class MiningPickaxePreservationAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public MiningPickaxePreservationAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Pickaxe Preservation";
    }

    @Override
    public String getDescription() {
        return "durability on all pickaxes";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double getPreservationChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
