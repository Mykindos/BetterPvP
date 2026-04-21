package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.doubletreasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("woodcutting_double_treasure_chance")
public class WoodcuttingDoubleTreasureChanceAttribute implements IProfessionAttribute {

    private final ProfessionProfileManager profileManager;

    @Inject
    public WoodcuttingDoubleTreasureChanceAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Double treasure chance";
    }

    @Override
    public String getDescription() {
        return "chance to double a successful woodcutting treasure drop";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    public double getChance(Player player) {
        return IProfessionAttribute.computeValue(player, "Woodcutting", this, profileManager);
    }
}
