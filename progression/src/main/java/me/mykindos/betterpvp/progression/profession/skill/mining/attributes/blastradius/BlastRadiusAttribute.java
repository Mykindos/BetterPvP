package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.blastradius;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("blast_radius")
public class BlastRadiusAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public BlastRadiusAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Blast Radius";
    }

    @Override
    public String getDescription() {
        return "increased Demolition Charge explosion radius";
    }

    @Override
    public String getOperation() {
        return " blocks";
    }

    public double getBonusRadius(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
