package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.oreinfusion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("ore_infusion")
public class OreInfusionAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public OreInfusionAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Ore Infusion";
    }

    @Override
    public String getDescription() {
        return "chance for nearby stone blocks to convert into ore when mining";
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
