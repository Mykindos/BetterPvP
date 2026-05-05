package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.compactedlog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("woodcutting_compacted_log")
public class WoodcuttingCompactedLogAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public WoodcuttingCompactedLogAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Compacted log chance";
    }

    @Override
    public String getDescription() {
        return "chance to find a Compacted Log when chopping natural logs";
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
        return IProfessionAttribute.computeValue(player, "Woodcutting", this, profileManager);
    }
}
