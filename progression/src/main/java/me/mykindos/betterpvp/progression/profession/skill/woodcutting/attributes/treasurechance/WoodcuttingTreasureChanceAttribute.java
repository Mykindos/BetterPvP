package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.treasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("woodcutting_treasure_chance")
public class WoodcuttingTreasureChanceAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public WoodcuttingTreasureChanceAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Treasure chance";
    }

    @Override
    public String getDescription() {
        return "chance to find woodcutting treasure after chopping natural logs";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    public double getChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Woodcutting", this, profileManager);
    }
}
