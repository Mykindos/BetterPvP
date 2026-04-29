package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.blastdamage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("blast_damage")
public class BlastDamageAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public BlastDamageAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Blast Damage";
    }

    @Override
    public String getDescription() {
        return "increased Demolition Charge explosion damage";
    }

    @Override
    public String getOperation() {
        return "";
    }

    public double getBonusDamage(Player player) {
        return IProfessionAttribute.computeValue(player, "Mining", this, profileManager);
    }
}
